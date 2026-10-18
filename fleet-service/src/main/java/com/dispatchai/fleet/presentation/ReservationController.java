package com.dispatchai.fleet.presentation;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.dispatchai.fleet.application.FleetReservationService;
import com.dispatchai.fleet.presentation.dto.ReservationRequest;
import com.dispatchai.fleet.presentation.dto.ReservationResponse;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
  private final FleetReservationService service;

  public ReservationController(FleetReservationService service) {
    this.service = service;
  }

  @PutMapping("/{id}")
  public ReservationResponse reserve(@PathVariable UUID id, @Valid @RequestBody ReservationRequest request) {
    return service.reserve(id, request);
  }

  @GetMapping("/{id}")
  public ReservationResponse get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PostMapping("/{id}/confirm")
  public ReservationResponse confirm(@PathVariable UUID id, @Valid @RequestBody ReservationRequest request) {
    return service.confirm(id, request);
  }

  @PostMapping("/{id}/release")
  public ReservationResponse release(@PathVariable UUID id, @Valid @RequestBody ReservationRequest request) {
    return service.release(id, request);
  }
}
