package com.dispatchai.dispatch.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dispatchai.dispatch.domain.model.RemoteDeliveryStatus;
import com.dispatchai.dispatch.domain.model.exceptions.DeliveryNotAssignableException;
import com.dispatchai.dispatch.infrastructure.delivery.DeliveryClient;
import com.dispatchai.dispatch.infrastructure.delivery.DeliveryResponse;
import com.dispatchai.dispatch.infrastructure.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.persistence.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.presentation.dto.AutoAssignRequest;

@ExtendWith(MockitoExtension.class)
class DispatchApplicationServiceTest {
  @Mock
  private FleetClient fleetClient;

  @Mock
  private DeliveryClient deliveryClient;

  @Mock
  private DeliveryAssignmentJpaRepository repository;

  @InjectMocks
  private DispatchApplicationService service;

  @Test
  void autoAssignRejectsNonPendingDelivery() {
    var deliveryId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(
            new DeliveryResponse(
                deliveryId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Sydney",
                "Wollongong",
                RemoteDeliveryStatus.ASSIGNED));

    assertThrows(
        DeliveryNotAssignableException.class,
        () -> service.autoAssign(
            new AutoAssignRequest(
                deliveryId,
                null)));

    verify(fleetClient, never())
        .getAvailableDrivers();

    verify(repository, never())
        .save(any());
  }
}
