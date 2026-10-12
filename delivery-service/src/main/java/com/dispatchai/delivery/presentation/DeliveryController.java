package com.dispatchai.delivery.presentation;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dispatchai.delivery.application.DeliveryApplicationService;
import com.dispatchai.delivery.presentation.dto.AssignDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.CancelDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.CreateDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.DelayDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.DeliveryResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
  private final DeliveryApplicationService deliveryService;

  public DeliveryController(DeliveryApplicationService deliveryService) {
    this.deliveryService = deliveryService;
  }

  @PostMapping
  public ResponseEntity<DeliveryResponse> create(@Valid @RequestBody CreateDeliveryRequest request) {
    var result = deliveryService.create(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @GetMapping("/{id}")
  public DeliveryResponse get(@PathVariable UUID id) {
    return deliveryService.get(id);
  }

  @PostMapping("/{id}/start")
  public DeliveryResponse start(@PathVariable UUID id) {
    return deliveryService.start(id);
  }

  @PostMapping("/{id}/complete")
  public DeliveryResponse complete(@PathVariable UUID id) {
    return deliveryService.complete(id);
  }

  @PostMapping("/{id}/cancel")
  public DeliveryResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancelDeliveryRequest request) {
    return deliveryService.cancel(id, request);
  }

  @PostMapping("/{id}/delay")
  public DeliveryResponse delay(
      @PathVariable UUID id,
      @Valid @RequestBody DelayDeliveryRequest request) {

    return deliveryService.delay(
        id,
        request.delayMinutes(),
        request.reason());
  }
}
