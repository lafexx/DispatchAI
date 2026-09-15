package com.dispatchai.delivery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dispatchai.delivery.domain.model.Delivery;
import com.dispatchai.delivery.domain.model.DeliveryStatus;
import com.dispatchai.delivery.infrastructure.persistence.DeliveryJpaRepository;

@ExtendWith(MockitoExtension.class)
class DeliveryApplicationServiceTest {
  @Mock
  private DeliveryJpaRepository repository;

  @InjectMocks
  private DeliveryApplicationService service;

  @Test
  void startLoadsDeliveryAndPersistsUpdatedState() {
    var id = UUID.randomUUID();

    var delivery = new Delivery(id, UUID.randomUUID(), "Sydney", "Wollongong");

    delivery.assign(UUID.randomUUID());

    when(repository.findById(id)).thenReturn(Optional.of(delivery));

    var result = service.start(id);

    assertEquals(DeliveryStatus.IN_TRANSIT.name(), result.status());

    verify(repository).save(delivery);
  }
}
