package com.dispatchai.fleet.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dispatchai.fleet.domain.model.DriverAvailability;
import com.dispatchai.fleet.domain.model.VehicleType;
import com.dispatchai.fleet.domain.model.Vehicle;
import com.dispatchai.fleet.domain.model.Driver;
import com.dispatchai.fleet.domain.model.FleetAssignmentStatus;
import com.dispatchai.fleet.domain.model.FleetReservationStatus;
import com.dispatchai.fleet.domain.exception.ReservationConflictException;
import com.dispatchai.fleet.infrastructure.persistence.store.FleetAssignmentStore;
import com.dispatchai.fleet.infrastructure.persistence.repository.FleetReservationJpaRepository;
import com.dispatchai.fleet.infrastructure.persistence.store.FleetReservationStore;
import com.dispatchai.fleet.infrastructure.persistence.repository.DriverJpaRepository;
import com.dispatchai.fleet.infrastructure.persistence.repository.VehicleJpaRepository;
import com.dispatchai.fleet.presentation.dto.SetDriverAvailabilityRequest;
import com.dispatchai.fleet.presentation.dto.SetVehicleAvailabilityRequest;
import com.dispatchai.fleet.presentation.dto.ReservationRequest;

@DataJpaTest(showSql = false)
@Import({ FleetAssignmentApplicationService.class, FleetAssignmentStore.class, FleetReservationService.class,
    FleetReservationStore.class, DriverApplicationService.class, VehicleApplicationService.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FleetAssignmentLifecycleIntegrationTest {
  @Autowired
  FleetAssignmentApplicationService lifecycle;
  @Autowired
  FleetReservationService service;
  @Autowired
  DriverApplicationService driverService;
  @Autowired
  VehicleApplicationService vehicleService;
  @Autowired
  DriverJpaRepository drivers;
  @Autowired
  VehicleJpaRepository vehicles;
  @Autowired
  FleetReservationJpaRepository reservations;
  @MockitoBean
  Clock clock;
  private final Instant now = Instant.parse("2026-10-12T01:00:00Z");

  @BeforeEach
  void time() {
    when(clock.instant()).thenReturn(now);
  }

  private UUID driver() {
    var value = new Driver(UUID.randomUUID(), "Demo");
    value.markAvailable();
    drivers.save(value);
    return value.getId();
  }

  private UUID vehicle() {
    var value = new Vehicle(UUID.randomUUID(), "TEST-" + UUID.randomUUID(), VehicleType.VAN, BigDecimal.valueOf(1200));
    value.markAvailable();
    vehicles.save(value);
    return value.getId();
  }

  private ReservationRequest request(UUID driver, UUID vehicle) {
    return new ReservationRequest(UUID.randomUUID(), driver, vehicle == null ? vehicle() : vehicle);
  }

  private boolean acquire(UUID id, ReservationRequest request, CountDownLatch start) throws InterruptedException {
    start.await();
    try {
      service.reserve(id, request);
      return true;
    } catch (ReservationConflictException ex) {
      return false;
    }
  }

  @Test
  void reservationOccupiesBothResourcesImmediatelyAndDuplicatesDoNotIncrement() {
    var driver = driver();
    var vehicle = vehicle();
    var request = request(driver, vehicle);
    var id = UUID.randomUUID();
    service.reserve(id, request);
    service.reserve(id, request);
    assertEquals(1, drivers.findById(driver).orElseThrow().getActiveAssignmentCount());
    assertFalse(driverService.get(driver).availability() == DriverAvailability.AVAILABLE);
    assertFalse(vehicleService.get(vehicle).available());
    service.confirm(id, request);
    service.confirm(id, request);
    lifecycle.assigned(request.deliveryId(), id, driver, vehicle, now);
    lifecycle.assigned(request.deliveryId(), id, driver, vehicle, now);
    assertEquals(1, drivers.findById(driver).orElseThrow().getActiveAssignmentCount());
    lifecycle.closed(request.deliveryId(), FleetAssignmentStatus.COMPLETED, now.plusSeconds(10));
    lifecycle.closed(request.deliveryId(), FleetAssignmentStatus.COMPLETED, now.plusSeconds(11));
    assertTrue(drivers.findById(driver).orElseThrow().isAvailable());
    assertTrue(vehicles.findById(vehicle).orElseThrow().isAvailable());
  }

  @Test
  void concurrentRequestsCannotReserveSameDriver() throws Exception {
    var driver = driver();
    var start = new CountDownLatch(1);
    boolean first, second;
    try (var pool = Executors.newFixedThreadPool(2)) {
      var a = pool.submit(() -> acquire(UUID.randomUUID(), request(driver, null), start));
      var b = pool.submit(() -> acquire(UUID.randomUUID(), request(driver, null), start));
      start.countDown();
      first = a.get(10, TimeUnit.SECONDS);
      second = b.get(10, TimeUnit.SECONDS);
    }
    assertNotEquals(first, second);
    assertEquals(1, drivers.findById(driver).orElseThrow().getActiveAssignmentCount());
  }

  @Test
  void concurrentRequestsCannotReserveSameVehicle() throws Exception {
    var vehicle = vehicle();
    var driverA = driver();
    var driverB = driver();
    var start = new CountDownLatch(1);
    boolean first, second;
    try (var pool = Executors.newFixedThreadPool(2)) {
      var a = pool.submit(() -> acquire(UUID.randomUUID(), request(driverA, vehicle), start));
      var b = pool.submit(() -> acquire(UUID.randomUUID(), request(driverB, vehicle), start));
      start.countDown();
      first = a.get(10, TimeUnit.SECONDS);
      second = b.get(10, TimeUnit.SECONDS);
    }
    assertNotEquals(first, second);
    assertEquals(1, vehicles.findById(vehicle).orElseThrow().getActiveAssignmentCount());
    assertEquals(1, drivers.findById(driverA).orElseThrow().getActiveAssignmentCount()
        + drivers.findById(driverB).orElseThrow().getActiveAssignmentCount());
  }

  @Test
  void duplicateTokenWithDifferentPayloadCannotChangeOwnership() {
    var driverA = driver();
    var driverB = driver();
    var request = request(driverA, null);
    var id = UUID.randomUUID();
    service.reserve(id, request);
    var other = new ReservationRequest(request.deliveryId(), driverB, null);
    assertThrows(ReservationConflictException.class, () -> service.reserve(id, other));
    assertThrows(ReservationConflictException.class, () -> service.release(id, other));
    assertTrue(drivers.findById(driverB).orElseThrow().isAvailable());
    assertEquals(driverA, service.get(id).driverId());
  }

  @Test
  void vehicleConflictRollsBackTheEntireReservation() {
    var driver = driver();
    var vehicle = vehicle();
    var id = UUID.randomUUID();
    vehicleService.setAvailability(vehicle, new SetVehicleAvailabilityRequest(false));
    assertThrows(ReservationConflictException.class, () -> service.reserve(id, request(driver, vehicle)));
    assertTrue(drivers.findById(driver).orElseThrow().isAvailable());
    assertEquals(0, drivers.findById(driver).orElseThrow().getActiveAssignmentCount());
    assertEquals(FleetReservationStatus.INITIALIZING, reservations.findById(id).orElseThrow().getStatus());
  }

  @Test
  void compensationBeforeDelayedReserveCreatesAReleaseTombstone() {
    var driver = driver();
    var request = request(driver, null);
    var id = UUID.randomUUID();
    service.release(id, request);
    service.release(id, request);
    assertThrows(ReservationConflictException.class, () -> service.reserve(id, request));
    assertEquals(FleetReservationStatus.RELEASED, service.get(id).status());
    assertTrue(drivers.findById(driver).orElseThrow().isAvailable());
  }

  @Test
  void unconfirmedLeaseExpiresAndCannotBeConfirmedOrReacquired() {
    var driver = driver();
    var vehicle = vehicle();
    var request = request(driver, vehicle);
    var id = UUID.randomUUID();
    service.reserve(id, request);
    when(clock.instant()).thenReturn(now.plusSeconds(121));
    service.expire(id, request.deliveryId());
    assertEquals("EXPIRED", service.get(id).releaseReason());
    assertThrows(ReservationConflictException.class, () -> service.confirm(id, request));
    assertThrows(ReservationConflictException.class, () -> service.reserve(id, request));
    assertTrue(drivers.findById(driver).orElseThrow().isAvailable());
    assertTrue(vehicles.findById(vehicle).orElseThrow().isAvailable());
  }

  @Test
  void confirmedAssignmentDoesNotExpireOrAcceptCompensation() {
    var driver = driver();
    var request = request(driver, null);
    var id = UUID.randomUUID();
    service.reserve(id, request);
    service.confirm(id, request);
    when(clock.instant()).thenReturn(now.plusSeconds(121));
    service.expire(id, request.deliveryId());
    assertEquals(FleetReservationStatus.CONFIRMED, service.get(id).status());
    assertThrows(ReservationConflictException.class, () -> service.release(id, request));
    assertFalse(drivers.findById(driver).orElseThrow().isAvailable());
    lifecycle.closed(request.deliveryId(), FleetAssignmentStatus.CANCELLED, now.plusSeconds(122));
    assertTrue(drivers.findById(driver).orElseThrow().isAvailable());
  }

  @Test
  void confirmAndCompensationRaceHasOnlyOneWinner() throws Exception {
    var driver = driver();
    var request = request(driver, null);
    var id = UUID.randomUUID();
    service.reserve(id, request);
    var start = new CountDownLatch(1);
    boolean confirmed, released;
    try (var pool = Executors.newFixedThreadPool(2)) {
      var a = pool.submit(() -> {
        start.await();
        try {
          service.confirm(id, request);
          return true;
        } catch (ReservationConflictException ex) {
          return false;
        }
      });
      var b = pool.submit(() -> {
        start.await();
        try {
          service.release(id, request);
          return true;
        } catch (ReservationConflictException ex) {
          return false;
        }
      });
      start.countDown();
      confirmed = a.get(10, TimeUnit.SECONDS);
      released = b.get(10, TimeUnit.SECONDS);
    }
    assertNotEquals(confirmed, released);
    assertEquals(confirmed ? 1 : 0, drivers.findById(driver).orElseThrow().getActiveAssignmentCount());
  }

  @Test
  void cancellationBeforeConfirmationReleasesTheHoldAndRejectsConfirm() {
    var driver = driver();
    var request = request(driver, null);
    var id = UUID.randomUUID();
    service.reserve(id, request);
    lifecycle.closed(request.deliveryId(), FleetAssignmentStatus.CANCELLED, now.plusSeconds(1));
    assertThrows(ReservationConflictException.class, () -> service.confirm(id, request));
    assertEquals("TERMINAL_DELIVERY", service.get(id).releaseReason());
    assertTrue(drivers.findById(driver).orElseThrow().isAvailable());
  }

  @Test
  void terminalBeforeReservationPreventsAcquisition() {
    var driver = driver();
    var request = request(driver, null);
    lifecycle.closed(request.deliveryId(), FleetAssignmentStatus.CANCELLED, now);
    assertThrows(ReservationConflictException.class, () -> service.reserve(UUID.randomUUID(), request));
    assertTrue(drivers.findById(driver).orElseThrow().isAvailable());
  }

  @Test
  void duplicateOldReleaseCannotReleaseANewerReservation() {
    var driver = driver();
    var old = request(driver, null);
    var oldId = UUID.randomUUID();
    service.reserve(oldId, old);
    service.release(oldId, old);
    var fresh = request(driver, null);
    var newId = UUID.randomUUID();
    service.reserve(newId, fresh);
    service.release(oldId, old);
    assertFalse(drivers.findById(driver).orElseThrow().isAvailable());
    assertEquals(FleetReservationStatus.RESERVED, service.get(newId).status());
  }

  @Test
  void manualUnavailabilityIsPreservedAfterConfirmedRelease() {
    var driver = driver();
    var vehicle = vehicle();
    var request = request(driver, vehicle);
    var id = UUID.randomUUID();
    service.reserve(id, request);
    service.confirm(id, request);
    assertEquals(DriverAvailability.UNAVAILABLE,
        driverService.setAvailability(driver, new SetDriverAvailabilityRequest(true)).availability());
    driverService.setAvailability(driver, new SetDriverAvailabilityRequest(false));
    vehicleService.setAvailability(vehicle, new SetVehicleAvailabilityRequest(false));
    lifecycle.closed(request.deliveryId(), FleetAssignmentStatus.COMPLETED, now);
    assertFalse(drivers.findById(driver).orElseThrow().isAvailable());
    assertFalse(vehicles.findById(vehicle).orElseThrow().isAvailable());
    assertEquals(0, drivers.findById(driver).orElseThrow().getActiveAssignmentCount());
  }

  @Test void capacityFailureLeavesBothResourcesUnoccupied() {
    var driver=driver(); var vehicle=vehicle(); var id=UUID.randomUUID();
    var request=new ReservationRequest(UUID.randomUUID(),driver,vehicle,new BigDecimal("1200.01"));
    assertThrows(ReservationConflictException.class,()->service.reserve(id,request));
    assertTrue(drivers.findById(driver).orElseThrow().isAvailable());
    assertTrue(vehicles.findById(vehicle).orElseThrow().isAvailable());
    assertEquals(0,drivers.findById(driver).orElseThrow().getActiveAssignmentCount());
  }
  @Test void exactCapacityCanBeReservedButChangedWeightCannotReuseToken() {
    var driver=driver(); var vehicle=vehicle(); var id=UUID.randomUUID();
    var request=new ReservationRequest(UUID.randomUUID(),driver,vehicle,new BigDecimal("1200"));
    service.reserve(id,request);
    assertThrows(ReservationConflictException.class,()->service.reserve(id,new ReservationRequest(request.deliveryId(),driver,vehicle,new BigDecimal("1199"))));
    assertThrows(ReservationConflictException.class,()->service.confirm(id,new ReservationRequest(request.deliveryId(),driver,vehicle,new BigDecimal("1199"))));
    assertEquals(FleetReservationStatus.RESERVED,service.get(id).status());
    service.confirm(id,request); assertEquals(FleetReservationStatus.CONFIRMED,service.get(id).status());
  }
  @Test void missingVehicleOrWeightCannotAcquireNewHold() {
    var driver=driver();
    assertThrows(ReservationConflictException.class,()->service.reserve(UUID.randomUUID(),new ReservationRequest(UUID.randomUUID(),driver,null,BigDecimal.ONE)));
    assertThrows(ReservationConflictException.class,()->service.reserve(UUID.randomUUID(),new ReservationRequest(UUID.randomUUID(),driver,vehicle(),null)));
    assertTrue(drivers.findById(driver).orElseThrow().isAvailable());
  }
}
