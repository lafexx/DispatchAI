package com.dispatchai.fleet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dispatchai.fleet.domain.model.Driver;
import com.dispatchai.fleet.domain.model.DriverAvailability;
import com.dispatchai.fleet.domain.model.exceptions.DriverNotFoundException;
import com.dispatchai.fleet.infrastructure.persistence.DriverJpaRepository;
import com.dispatchai.fleet.presentation.dto.CreateDriverRequest;
import com.dispatchai.fleet.presentation.dto.SetDriverAvailabilityRequest;

@ExtendWith(MockitoExtension.class)
class DriverApplicationServiceTest {

  @Mock
  private DriverJpaRepository repository;

  @InjectMocks
  private DriverApplicationService service;

  @Test
  void createPersistsDriverAndReturnsResponse() {
    var request = new CreateDriverRequest(
        "Alex Smith");

    var result = service.create(request);

    assertEquals("Alex Smith", result.name());
    assertEquals(
        DriverAvailability.UNAVAILABLE,
        result.availability());

    verify(repository)
        .save(any(Driver.class));
  }

  @Test
  void createMapsRequestToPersistedDriver() {
    var request = new CreateDriverRequest(
        "Alex Smith");

    service.create(request);

    var captor = ArgumentCaptor.forClass(Driver.class);

    verify(repository).save(captor.capture());

    var driver = captor.getValue();

    assertEquals("Alex Smith", driver.getName());
    assertEquals(
        DriverAvailability.UNAVAILABLE,
        driver.getAvailability());
  }

  @Test
  void getReturnsExistingDriver() {
    var id = UUID.randomUUID();

    var driver = new Driver(
        id,
        "Alex Smith");

    when(repository.findById(id))
        .thenReturn(Optional.of(driver));

    var result = service.get(id);

    assertEquals(id, result.id());
    assertEquals("Alex Smith", result.name());
    assertEquals(
        DriverAvailability.UNAVAILABLE,
        result.availability());
  }

  @Test
  void getThrowsWhenDriverDoesNotExist() {
    var id = UUID.randomUUID();

    when(repository.findById(id))
        .thenReturn(Optional.empty());

    assertThrows(
        DriverNotFoundException.class,
        () -> service.get(id));
  }

  @Test
  void availableReturnsOnlyAvailableDrivers() {
    var availableDriver = new Driver(
        UUID.randomUUID(),
        "Alex Smith");

    var unavailableDriver = new Driver(
        UUID.randomUUID(),
        "Jamie Jones");

    availableDriver.markAvailable();

    when(repository.findAll())
        .thenReturn(
            List.of(
                availableDriver,
                unavailableDriver));

    var result = service.available();

    assertEquals(1, result.size());

    assertEquals(
        availableDriver.getId(),
        result.getFirst().id());

    assertEquals(
        "Alex Smith",
        result.getFirst().name());

    assertEquals(
        DriverAvailability.AVAILABLE,
        result.getFirst().availability());
  }

  @Test
  void availableReturnsEmptyListWhenNoDriversAreAvailable() {
    var first = new Driver(
        UUID.randomUUID(),
        "Alex Smith");

    var second = new Driver(
        UUID.randomUUID(),
        "Jamie Jones");

    when(repository.findAll())
        .thenReturn(List.of(first, second));

    var result = service.available();

    assertEquals(0, result.size());
  }

  @Test
  void setAvailabilityMarksDriverAvailable() {
    var id = UUID.randomUUID();

    var driver = new Driver(
        id,
        "Alex Smith");

    when(repository.findById(id))
        .thenReturn(Optional.of(driver));

    var request = new SetDriverAvailabilityRequest(true);

    var result = service.setAvailability(id, request);

    assertEquals(
        DriverAvailability.AVAILABLE,
        result.availability());

    verify(repository).save(driver);
  }

  @Test
  void setAvailabilityMarksDriverUnavailable() {
    var id = UUID.randomUUID();

    var driver = new Driver(
        id,
        "Alex Smith");

    driver.markAvailable();

    when(repository.findById(id))
        .thenReturn(Optional.of(driver));

    var request = new SetDriverAvailabilityRequest(false);

    var result = service.setAvailability(id, request);

    assertEquals(
        DriverAvailability.UNAVAILABLE,
        result.availability());

    verify(repository).save(driver);
  }

  @Test
  void setAvailabilityThrowsWhenDriverDoesNotExist() {
    var id = UUID.randomUUID();

    when(repository.findById(id))
        .thenReturn(Optional.empty());

    var request = new SetDriverAvailabilityRequest(true);

    assertThrows(
        DriverNotFoundException.class,
        () -> service.setAvailability(
            id,
            request));

    verify(repository, never())
        .save(any());
  }

  @Test
  void markUnavailableMarksAvailableDriverUnavailable() {
    var id = UUID.randomUUID();

    var driver = new Driver(
        id,
        "Alex Smith");

    driver.markAvailable();

    when(repository.findById(id))
        .thenReturn(Optional.of(driver));

    var result = service.markUnavailable(id);

    assertEquals(
        DriverAvailability.UNAVAILABLE,
        result.availability());

    verify(repository).save(driver);
  }

  @Test
  void markUnavailableLeavesDriverUnavailable() {
    var id = UUID.randomUUID();

    var driver = new Driver(
        id,
        "Alex Smith");

    when(repository.findById(id))
        .thenReturn(Optional.of(driver));

    var result = service.markUnavailable(id);

    assertEquals(
        DriverAvailability.UNAVAILABLE,
        result.availability());

    verify(repository).save(driver);
  }

  @Test
  void markUnavailableThrowsWhenDriverDoesNotExist() {
    var id = UUID.randomUUID();

    when(repository.findById(id))
        .thenReturn(Optional.empty());

    assertThrows(
        DriverNotFoundException.class,
        () -> service.markUnavailable(id));

    verify(repository, never())
        .save(any());
  }
}
