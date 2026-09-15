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

import com.dispatchai.fleet.application.DriverApplicationService;
import com.dispatchai.fleet.presentation.dto.CreateDriverRequest;
import com.dispatchai.fleet.presentation.dto.DriverResponse;
import com.dispatchai.fleet.presentation.dto.SetDriverAvailabilityRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {
  private final DriverApplicationService driverService;

  public DriverController(DriverApplicationService driverService) {
    this.driverService = driverService;
  }

  @PostMapping
  public ResponseEntity<DriverResponse> create(@Valid @RequestBody CreateDriverRequest request) {
    var result = driverService.create(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @GetMapping("/{id}")
  public DriverResponse get(@PathVariable UUID id) {
    return driverService.get(id);
  }

  @GetMapping("/available")
  public List<DriverResponse> available() {
    return driverService.available();
  }

  @PostMapping("/{id}/availability")
  public DriverResponse setAvailability(@PathVariable UUID id,
      @Valid @RequestBody SetDriverAvailabilityRequest request) {
    return driverService.setAvailability(id, request);
  }
}
