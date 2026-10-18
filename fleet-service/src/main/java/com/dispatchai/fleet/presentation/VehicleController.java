package com.dispatchai.fleet.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dispatchai.fleet.application.VehicleApplicationService;
import com.dispatchai.fleet.presentation.dto.CreateVehicleRequest;
import com.dispatchai.fleet.presentation.dto.SetVehicleAvailabilityRequest;
import com.dispatchai.fleet.presentation.dto.VehicleResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
  private final VehicleApplicationService vehicleService;

  public VehicleController(VehicleApplicationService vehicleService) {
    this.vehicleService = vehicleService;
  }

  @PostMapping
  public ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
    var result = vehicleService.create(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @GetMapping("/{id}")
  public VehicleResponse get(@PathVariable UUID id) {
    return vehicleService.get(id);
  }

  @GetMapping
  public List<VehicleResponse> get() {
    return vehicleService.get();
  }

  @PostMapping("/{id}/availability")
  public VehicleResponse setAvailability(@PathVariable UUID id,
      @Valid @RequestBody SetVehicleAvailabilityRequest request) {
    return vehicleService.setAvailability(id, request);
  }
}
