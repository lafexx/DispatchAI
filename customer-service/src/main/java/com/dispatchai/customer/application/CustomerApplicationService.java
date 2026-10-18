package com.dispatchai.customer.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dispatchai.customer.domain.exception.CustomerNotFoundException;
import com.dispatchai.customer.domain.model.Customer;
import com.dispatchai.customer.infrastructure.persistence.repository.CustomerJpaRepository;
import com.dispatchai.customer.presentation.dto.CreateCustomerRequest;
import com.dispatchai.customer.presentation.dto.CustomerResponse;

@Service
public class CustomerApplicationService {
  private final CustomerJpaRepository repository;

  public CustomerApplicationService(CustomerJpaRepository repository) {
    this.repository = repository;
  }

  public CustomerResponse create(CreateCustomerRequest request) {
    var customer = new Customer(
        UUID.randomUUID(),
        request.name(),
        request.email(),
        request.phone());

    repository.save(customer);

    return toResponse(customer);
  }

  public CustomerResponse get(UUID id) {
    var customer = repository.findById(id)
        .orElseThrow(() -> new CustomerNotFoundException(id));

    return toResponse(customer);
  }

  public List<CustomerResponse> getAll() {
    var customers = repository.findAll();

    return toResponse(customers);
  }

  private CustomerResponse toResponse(Customer customer) {
    return new CustomerResponse(
        customer.getId(),
        customer.getName(),
        customer.getEmail(),
        customer.getPhone());
  }

  private List<CustomerResponse> toResponse(List<Customer> customers) {
    List<CustomerResponse> responses = new ArrayList<CustomerResponse>();

    for (Customer customer : customers) {
      responses.add(new CustomerResponse(
          customer.getId(),
          customer.getName(),
          customer.getEmail(),
          customer.getPhone()));
    }

    return responses;
  }
}
