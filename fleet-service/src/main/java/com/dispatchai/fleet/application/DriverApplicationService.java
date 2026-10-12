package com.dispatchai.fleet.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dispatchai.fleet.domain.model.Driver;
import com.dispatchai.fleet.domain.model.DriverAvailability;
import com.dispatchai.fleet.domain.model.exceptions.DriverNotFoundException;
import com.dispatchai.fleet.infrastructure.persistence.DriverJpaRepository;
import com.dispatchai.fleet.presentation.dto.CreateDriverRequest;
import com.dispatchai.fleet.presentation.dto.DriverResponse;
import com.dispatchai.fleet.presentation.dto.SetDriverAvailabilityRequest;

@Service
public class DriverApplicationService {
  private final DriverJpaRepository repository;

  public DriverApplicationService(DriverJpaRepository repository) {
    this.repository = repository;
  }

  public DriverResponse create(CreateDriverRequest request) {
    var driver = new Driver(UUID.randomUUID(), request.name());

    repository.save(driver);

    return toResponse(driver);
  }

  public DriverResponse get(UUID id) {
    var driver = repository.findById(id)
        .orElseThrow(() -> new DriverNotFoundException(id));

    return toResponse(driver);
  }

  public List<DriverResponse> available() {
    var drivers = repository.findAll().stream()
        .filter(d -> d.getAvailability() == DriverAvailability.AVAILABLE).toList();

    return toResponse(drivers);
  }

  public DriverResponse setAvailability(UUID id, SetDriverAvailabilityRequest request) {
    var driver = repository.findById(id)
        .orElseThrow(() -> new DriverNotFoundException(id));

    if (request.available()) {
      driver.markAvailable();
    } else {
      driver.markUnavailable();
    }

    repository.save(driver);

    return toResponse(driver);
  }

  public DriverResponse markUnavailable(UUID driverId) {
    var driver = repository.findById(driverId)
        .orElseThrow(() -> new DriverNotFoundException(driverId));

    driver.markUnavailable();

    repository.save(driver);

    return toResponse(driver);
  }

  public List<DriverResponse> getAll() {
    var drivers = repository.findAll();

    return toResponse(drivers);
  }

  private DriverResponse toResponse(Driver driver) {
    return new DriverResponse(driver.getId(), driver.getName(), driver.getAvailability());
  }

  private List<DriverResponse> toResponse(List<Driver> drivers) {
    List<DriverResponse> responses = new ArrayList<DriverResponse>();

    for (Driver driver : drivers) {
      responses.add(toResponse(driver));
    }

    return responses;
  }
}
