package com.dispatchai.dispatch.presentation;

import java.util.UUID;

import org.apache.catalina.connector.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dispatchai.dispatch.application.DispatchApplicationService;
import com.dispatchai.dispatch.presentation.dto.AssignmentResponse;
import com.dispatchai.dispatch.presentation.dto.AutoAssignRequest;
import com.dispatchai.dispatch.presentation.dto.CreateAssignmentRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {
  private final DispatchApplicationService dispatchService;

  public DispatchController(DispatchApplicationService dispatchService) {
    this.dispatchService = dispatchService;
  }

  @PostMapping("/assignments")
  public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request) {
    var result = dispatchService.create(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @GetMapping("/assignments/{id}")
  public AssignmentResponse getByAssignmentId(@PathVariable UUID id) {
    return dispatchService.getByAssignmentId(id);
  }

  @GetMapping("/deliveries/{deliveryId}/assignment")
  public AssignmentResponse getByDeliveryId(@PathVariable UUID deliveryId) {
    return dispatchService.getByDeliveryId(deliveryId);
  }

  @PostMapping("/assignments/auto")
  public ResponseEntity<AssignmentResponse> autoAssign(@Valid @RequestBody AutoAssignRequest request) {
    var result = dispatchService.autoAssign(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }
}
