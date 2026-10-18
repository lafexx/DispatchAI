package com.dispatchai.fleet.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dispatchai.fleet.domain.model.Vehicle;
import com.dispatchai.fleet.domain.exception.VehicleNotFoundException;
import com.dispatchai.fleet.infrastructure.persistence.repository.VehicleJpaRepository;
import com.dispatchai.fleet.presentation.dto.CreateVehicleRequest;
import com.dispatchai.fleet.presentation.dto.SetVehicleAvailabilityRequest;
import com.dispatchai.fleet.presentation.dto.VehicleResponse;

@Service
public class VehicleApplicationService {
  private final VehicleJpaRepository repository;

  public VehicleApplicationService(VehicleJpaRepository repository) {
    this.repository = repository;
  }

  public VehicleResponse create(CreateVehicleRequest request) {
    var vehicle = new Vehicle(UUID.randomUUID(), request.registration(), request.type(), request.capacityKg());

    repository.save(vehicle);

    return toResponse(vehicle);
  }

  public VehicleResponse get(UUID id) {
    var vehicle = repository.findById(id)
        .orElseThrow(() -> new VehicleNotFoundException(id));

    return toResponse(vehicle);
  }

  public List<VehicleResponse> get() {
    var vehicles = repository.findAll();

    return toResponse(vehicles);
  }

  @Transactional
  public VehicleResponse setAvailability(UUID id, SetVehicleAvailabilityRequest request) {
    var vehicle = repository.findByIdForUpdate(id)
        .orElseThrow(() -> new VehicleNotFoundException(id));

    if (request.available()) {
      vehicle.markAvailable();
    } else {
      vehicle.markUnavailable();
    }

    repository.save(vehicle);

    return toResponse(vehicle);
  }

  public VehicleResponse toResponse(Vehicle vehicle) {
    return new VehicleResponse(vehicle.getId(), vehicle.getRegistration(), vehicle.getType(), vehicle.getCapacityKg(),
        vehicle.isAvailable());
  }

  public List<VehicleResponse> toResponse(List<Vehicle> vehicles) {
    List<VehicleResponse> responses = new ArrayList<VehicleResponse>();

    for (Vehicle vehicle : vehicles) {
      responses.add(new VehicleResponse(vehicle.getId(), vehicle.getRegistration(), vehicle.getType(),
          vehicle.getCapacityKg(), vehicle.isAvailable()));
    }

    return responses;
  }
}
