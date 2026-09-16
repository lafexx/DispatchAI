package com.dispatchai.customer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dispatchai.customer.domain.exceptions.CustomerNotFoundException;
import com.dispatchai.customer.domain.model.Customer;
import com.dispatchai.customer.infrastructure.persistence.CustomerJpaRepository;
import com.dispatchai.customer.presentation.dto.CreateCustomerRequest;

@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceTest {

  @Mock
  private CustomerJpaRepository repository;

  @InjectMocks
  private CustomerApplicationService service;

  @Test
  void createPersistsCustomerAndReturnsResponse() {
    var request = new CreateCustomerRequest(
        "Alex Smith",
        "alex@example.com",
        "0400000000");

    var result = service.create(request);

    assertEquals("Alex Smith", result.name());
    assertEquals("alex@example.com", result.email());
    assertEquals("0400000000", result.phone());

    verify(repository).save(any(Customer.class));
  }

  @Test
  void createMapsRequestToPersistedCustomer() {
    var request = new CreateCustomerRequest(
        "Alex Smith",
        "alex@example.com",
        "0400000000");

    service.create(request);

    verify(repository).save(
        org.mockito.ArgumentMatchers.argThat(customer -> customer.getId() != null
            && customer.getName().equals("Alex Smith")
            && customer.getEmail().equals("alex@example.com")
            && customer.getPhone().equals("0400000000")));
  }

  @Test
  void getReturnsExistingCustomer() {
    var id = UUID.randomUUID();

    var customer = new Customer(
        id,
        "Alex Smith",
        "alex@example.com",
        "0400000000");

    when(repository.findById(id))
        .thenReturn(Optional.of(customer));

    var result = service.get(id);

    assertEquals(id, result.id());
    assertEquals("Alex Smith", result.name());
    assertEquals("alex@example.com", result.email());
    assertEquals("0400000000", result.phone());
  }

  @Test
  void getThrowsWhenCustomerDoesNotExist() {
    var id = UUID.randomUUID();

    when(repository.findById(id))
        .thenReturn(Optional.empty());

    assertThrows(
        CustomerNotFoundException.class,
        () -> service.get(id));
  }

  @Test
  void getAllReturnsMappedCustomers() {
    var firstId = UUID.randomUUID();
    var secondId = UUID.randomUUID();

    var first = new Customer(
        firstId,
        "Alex Smith",
        "alex@example.com",
        "0400000000");

    var second = new Customer(
        secondId,
        "Jamie Jones",
        "jamie@example.com",
        null);

    when(repository.findAll())
        .thenReturn(List.of(first, second));

    var result = service.getAll();

    assertEquals(2, result.size());

    assertEquals(firstId, result.get(0).id());
    assertEquals("Alex Smith", result.get(0).name());
    assertEquals("alex@example.com", result.get(0).email());
    assertEquals("0400000000", result.get(0).phone());

    assertEquals(secondId, result.get(1).id());
    assertEquals("Jamie Jones", result.get(1).name());
    assertEquals("jamie@example.com", result.get(1).email());
    assertEquals(null, result.get(1).phone());
  }

  @Test
  void getAllReturnsEmptyListWhenNoCustomersExist() {
    when(repository.findAll())
        .thenReturn(List.of());

    var result = service.getAll();

    assertEquals(0, result.size());
  }
}
