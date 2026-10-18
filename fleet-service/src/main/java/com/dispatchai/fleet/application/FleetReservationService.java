package com.dispatchai.fleet.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dispatchai.fleet.domain.model.FleetReservation;
import com.dispatchai.fleet.domain.model.FleetReservationStatus;
import com.dispatchai.fleet.domain.exception.ReservationNotFoundException;
import com.dispatchai.fleet.domain.exception.VehicleNotFoundException;
import com.dispatchai.fleet.domain.exception.DriverNotFoundException;
import com.dispatchai.fleet.domain.exception.ReservationConflictException;
import com.dispatchai.fleet.infrastructure.persistence.store.FleetAssignmentStore;
import com.dispatchai.fleet.infrastructure.persistence.repository.FleetReservationJpaRepository;
import com.dispatchai.fleet.infrastructure.persistence.store.FleetReservationStore;
import com.dispatchai.fleet.infrastructure.persistence.repository.DriverJpaRepository;
import com.dispatchai.fleet.infrastructure.persistence.repository.VehicleJpaRepository;
import com.dispatchai.fleet.presentation.dto.ReservationRequest;
import com.dispatchai.fleet.presentation.dto.ReservationResponse;

@Service
public class FleetReservationService {
  private final FleetAssignmentStore deliveries;
  private final FleetReservationStore reservations;
  private final FleetReservationJpaRepository repository;
  private final DriverJpaRepository drivers;
  private final VehicleJpaRepository vehicles;
  private final Clock clock;
  private final long leaseSeconds;

  public FleetReservationService(FleetAssignmentStore deliveries, FleetReservationStore reservations,
      FleetReservationJpaRepository repository, DriverJpaRepository drivers, VehicleJpaRepository vehicles,
      Clock clock, @Value("${fleet.reservation.lease-seconds:120}") long leaseSeconds) {
    if (leaseSeconds <= 0) {
      throw new IllegalArgumentException("Reservation lease must be positive");
    }
    this.deliveries = deliveries;
    this.reservations = reservations;
    this.repository = repository;
    this.drivers = drivers;
    this.vehicles = vehicles;
    this.clock = clock;
    this.leaseSeconds = leaseSeconds;
  }

  @Transactional
  public ReservationResponse reserve(UUID id, ReservationRequest request) {
    if (request.vehicleId() == null) throw new ReservationConflictException("A vehicle is required for new reservations");
    if (request.parcelWeightKg() == null) throw new ReservationConflictException("Parcel weight is required for new reservations");
    var delivery = deliveries.lock(request.deliveryId());
    var reservation = reservations.lock(id);
    reservation.bind(request.deliveryId(), request.driverId(), request.vehicleId());
    reservation.bindWeight(request.parcelWeightKg());
    if (delivery.getStatus().isTerminal()) {
      throw new ReservationConflictException("Delivery is already terminal");
    }
    if (reservation.getStatus() == FleetReservationStatus.RESERVED
        || reservation.getStatus() == FleetReservationStatus.CONFIRMED) {
      if (reservation.expired(clock.instant())) {
        throw new ReservationConflictException("Reservation has expired");
      }
      return response(reservation); // Same token and exact payload: no extra occupancy.
    }
    if (reservation.getStatus() == FleetReservationStatus.RELEASED) {
      throw new ReservationConflictException("Reservation has already been released");
    }
    var occupied = repository.findByDeliveryIdAndStatusIn(request.deliveryId(),
        List.of(FleetReservationStatus.RESERVED, FleetReservationStatus.CONFIRMED));
    if (!occupied.isEmpty()) {
      throw new ReservationConflictException("Delivery already has a reservation");
    }
    // Availability checks and both increments occur under the same resource locks.
    var driver = drivers.findByIdForUpdate(request.driverId())
        .orElseThrow(() -> new DriverNotFoundException(request.driverId()));
    if (!driver.isAvailable()) {
      throw new ReservationConflictException("Driver is not available");
    }
    if (request.vehicleId() != null) {
      var vehicle = vehicles.findByIdForUpdate(request.vehicleId())
          .orElseThrow(() -> new VehicleNotFoundException(request.vehicleId()));
      if (!vehicle.isAvailable()) {
        throw new ReservationConflictException("Vehicle is not available");
      }
      if (vehicle.getCapacityKg().compareTo(request.parcelWeightKg()) < 0)
        throw new ReservationConflictException("Vehicle capacity is below parcel weight");
      vehicle.assignmentStarted();
    }
    driver.assignmentStarted();
    reservation.reserve(clock.instant().plusSeconds(leaseSeconds));
    return response(reservation);
  }

  @Transactional
  public ReservationResponse confirm(UUID id, ReservationRequest request) {
    var delivery = deliveries.lock(request.deliveryId());
    var reservation = reservations.lock(id);
    reservation.bind(request.deliveryId(), request.driverId(), request.vehicleId());
    if (delivery.getStatus().isTerminal()) {
      throw new ReservationConflictException("Delivery is terminal");
    }
    reservation.verifyWeight(request.parcelWeightKg());
    reservation.confirm(clock.instant());
    return response(reservation);
  }

  @Transactional
  public ReservationResponse release(UUID id, ReservationRequest request) {
    deliveries.lock(request.deliveryId());
    var reservation = reservations.lock(id);
    reservation.bind(request.deliveryId(), request.driverId(), request.vehicleId());
    // An unknown token becomes a tombstone. A delayed reserve request cannot
    // acquire resources after compensation has already completed.
    if (reservation.release(false)) {
      releaseResources(reservation);
    }
    return response(reservation);
  }

  @Transactional
  public void expire(UUID id, UUID deliveryId) {
    deliveries.lock(deliveryId);
    var reservation = reservations.lock(id);
    if (reservation.expired(clock.instant()) && reservation.release(false, "EXPIRED")) {
      releaseResources(reservation);
    }
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void closeForDelivery(UUID deliveryId) {
    // Caller already owns the delivery lifecycle lock. All other reservation
    // operations use that same lock before touching reservation/resource state.
    for (var reservation : repository.findByDeliveryIdAndStatusIn(deliveryId,
        List.of(FleetReservationStatus.RESERVED, FleetReservationStatus.CONFIRMED))) {
      if (reservation.release(true)) {
        releaseResources(reservation);
      }
    }
  }

  @Transactional(readOnly = true)
  public ReservationResponse get(UUID id) {
    var reservation = repository.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
    if (reservation.getStatus() == FleetReservationStatus.INITIALIZING) {
      throw new ReservationNotFoundException(id);
    }
    return response(reservation);
  }

  private void releaseResources(FleetReservation reservation) {
    var driverId = reservation.getDriverId();
    drivers.findByIdForUpdate(driverId).orElseThrow(() -> new DriverNotFoundException(driverId)).assignmentFinished();
    var vehicleId = reservation.getVehicleId();
    if (vehicleId != null) {
      vehicles.findByIdForUpdate(vehicleId).orElseThrow(() -> new VehicleNotFoundException(vehicleId))
          .assignmentFinished();
    }
  }

  private ReservationResponse response(FleetReservation reservation) {
    return new ReservationResponse(reservation.getAssignmentId(), reservation.getDeliveryId(),
        reservation.getDriverId(), reservation.getVehicleId(), reservation.getStatus(), reservation.getExpiresAt(),
        reservation.getReleaseReason());
  }
}
