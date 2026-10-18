package com.dispatchai.customer.presentation;

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

import com.dispatchai.customer.application.CustomerApplicationService;
import com.dispatchai.customer.application.NotificationApplicationService;
import com.dispatchai.customer.presentation.dto.CreateCustomerRequest;
import com.dispatchai.customer.presentation.dto.CustomerResponse;
import com.dispatchai.customer.presentation.dto.NotificationResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

  private final CustomerApplicationService customerService;
  private final NotificationApplicationService notificationService;

  public CustomerController(
      CustomerApplicationService customerService,
      NotificationApplicationService notificationService) {

    this.customerService = customerService;
    this.notificationService = notificationService;
  }

  @PostMapping
  public ResponseEntity<CustomerResponse> create(
      @Valid @RequestBody CreateCustomerRequest request) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(customerService.create(request));
  }

  @GetMapping("/{id}")
  public CustomerResponse get(@PathVariable UUID id) {
    return customerService.get(id);
  }

  @GetMapping
  public List<CustomerResponse> getAll() {
    return customerService.getAll();
  }

  @GetMapping("/{id}/notifications")
  public List<NotificationResponse> notifications(
      @PathVariable UUID id) {

    return notificationService.getForCustomer(id);
  }
}
