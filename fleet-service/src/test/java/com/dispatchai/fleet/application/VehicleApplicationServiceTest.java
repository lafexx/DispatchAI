package com.dispatchai.fleet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dispatchai.fleet.domain.model.Vehicle;
import com.dispatchai.fleet.domain.model.VehicleType;
import com.dispatchai.fleet.domain.model.exceptions.VehicleNotFoundException;
import com.dispatchai.fleet.infrastructure.persistence.VehicleJpaRepository;
import com.dispatchai.fleet.presentation.dto.CreateVehicleRequest;
import com.dispatchai.fleet.presentation.dto.SetVehicleAvailabilityRequest;

@ExtendWith(MockitoExtension.class)
class VehicleApplicationServiceTest {

  @Mock
  private VehicleJpaRepository repository;

  @InjectMocks
  private VehicleApplicationService service;

  @Test
  void createPersistsVehicleAndReturnsResponse() {
    var request = new CreateVehicleRequest(
        "ABC123",
        VehicleType.VAN,
        new BigDecimal("1200"));

    var result = service.create(request);

    assertEquals("ABC123", result.registration());
    assertEquals(VehicleType.VAN, result.type());
    assertEquals(new BigDecimal("1200"), result.capacityKg());
    assertFalse(result.available());

    verify(repository)
        .save(any(Vehicle.class));
  }

  @Test
  void createMapsRequestToPersistedVehicle() {
    var request = new CreateVehicleRequest(
        "ABC123",
        VehicleType.VAN,
        new BigDecimal("1200"));

    service.create(request);

    var captor = ArgumentCaptor.forClass(Vehicle.class);

    verify(repository).save(captor.capture());

    var vehicle = captor.getValue();

    assertEquals("ABC123", vehicle.getRegistration());
    assertEquals(VehicleType.VAN, vehicle.getType());
    assertEquals(new BigDecimal("1200"), vehicle.getCapacityKg());
    assertFalse(vehicle.isAvailable());
  }

  @Test
  void getReturnsExistingVehicle() {
    var id = UUID.randomUUID();

    var vehicle = new Vehicle(
        id,
        "ABC123",
        VehicleType.VAN,
        new BigDecimal("1200"));

    when(repository.findById(id))
        .thenReturn(Optional.of(vehicle));

    var result = service.get(id);

    assertEquals(id, result.id());
    assertEquals("ABC123", result.registration());
    assertEquals(VehicleType.VAN, result.type());
    assertEquals(new BigDecimal("1200"), result.capacityKg());
    assertFalse(result.available());
  }

  @Test
  void getThrowsWhenVehicleDoesNotExist() {
    var id = UUID.randomUUID();

    when(repository.findById(id))
        .thenReturn(Optional.empty());

    assertThrows(
        VehicleNotFoundException.class,
        () -> service.get(id));
  }

  @Test
  void getReturnsAllVehicles() {
    var firstId = UUID.randomUUID();
    var secondId = UUID.randomUUID();

    var first = new Vehicle(
        firstId,
        "ABC123",
        VehicleType.VAN,
        new BigDecimal("1200"));

    var second = new Vehicle(
        secondId,
        "XYZ789",
        VehicleType.TRUCK,
        new BigDecimal("5000"));

    second.markAvailable();

    when(repository.findAll())
        .thenReturn(List.of(first, second));

    var result = service.get();

    assertEquals(2, result.size());

    assertEquals(firstId, result.get(0).id());
    assertEquals("ABC123", result.get(0).registration());
    assertEquals(VehicleType.VAN, result.get(0).type());
    assertEquals(new BigDecimal("1200"), result.get(0).capacityKg());
    assertFalse(result.get(0).available());

    assertEquals(secondId, result.get(1).id());
    assertEquals("XYZ789", result.get(1).registration());
    assertEquals(VehicleType.TRUCK, result.get(1).type());
    assertEquals(new BigDecimal("5000"), result.get(1).capacityKg());
    assertTrue(result.get(1).available());
  }

  @Test
  void getReturnsEmptyListWhenNoVehiclesExist() {
    when(repository.findAll())
        .thenReturn(List.of());

    var result = service.get();

    assertEquals(0, result.size());
  }

  @Test
  void setAvailabilityMarksVehicleAvailable() {
    var id = UUID.randomUUID();

    var vehicle = new Vehicle(
        id,
        "ABC123",
        VehicleType.VAN,
        new BigDecimal("1200"));

    when(repository.findById(id))
        .thenReturn(Optional.of(vehicle));

    var request = new SetVehicleAvailabilityRequest(true);

    var result = service.setAvailability(id, request);

    assertTrue(result.available());
    assertTrue(vehicle.isAvailable());

    verify(repository).save(vehicle);
  }

  @Test
  void setAvailabilityMarksVehicleUnavailable() {
    var id = UUID.randomUUID();

    var vehicle = new Vehicle(
        id,
        "ABC123",
        VehicleType.VAN,
        new BigDecimal("1200"));

    vehicle.markAvailable();

    when(repository.findById(id))
        .thenReturn(Optional.of(vehicle));

    var request = new SetVehicleAvailabilityRequest(false);

    var result = service.setAvailability(id, request);

    assertFalse(result.available());
    assertFalse(vehicle.isAvailable());

    verify(repository).save(vehicle);
  }

  @Test
  void setAvailabilityThrowsWhenVehicleDoesNotExist() {
    var id = UUID.randomUUID();

    when(repository.findById(id))
        .thenReturn(Optional.empty());

    var request = new SetVehicleAvailabilityRequest(true);

    assertThrows(
        VehicleNotFoundException.class,
        () -> service.setAvailability(id, request));

    verify(repository, never())
        .save(any());
  }
}
