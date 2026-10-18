package com.dispatchai.dispatch.infrastructure.scheduling;

import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.dispatchai.dispatch.application.DispatchApplicationService;
import com.dispatchai.dispatch.application.ReservationCleanupService;
import com.dispatchai.dispatch.domain.model.AssignmentStatus;
import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.RemoteDeliveryStatus;
import com.dispatchai.dispatch.infrastructure.client.delivery.DeliveryClient;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.DeliveryResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationResponse;
import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.infrastructure.persistence.repository.ReservationCleanupJpaRepository;

class DispatchReservationMaintenanceTest {
  @Test
  void terminalReleaseDoesNotBecomeARejectionWhileTerminalEventIsInFlight() {
    var assignments = mock(DeliveryAssignmentJpaRepository.class);
    var cleanups = mock(ReservationCleanupJpaRepository.class);
    var service = mock(DispatchApplicationService.class);
    var cleanup = mock(ReservationCleanupService.class);
    var fleet = mock(FleetClient.class);
    var delivery = mock(DeliveryClient.class);
    var assignment = new DeliveryAssignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);
    when(cleanups.findAll()).thenReturn(List.of());
    when(assignments.findByStatus(AssignmentStatus.ACTIVE)).thenReturn(List.of(assignment));
    when(fleet.reservation(assignment.getId())).thenReturn(new ReservationResponse(assignment.getId(),
        assignment.getDeliveryId(), assignment.getDriverId(), null, "RELEASED", Instant.now(), "TERMINAL_DELIVERY"));
    new DispatchReservationMaintenance(assignments, cleanups, service, cleanup, fleet, delivery).maintain();
    verifyNoInteractions(service, delivery);
  }

  @Test
  void expiryRejectsOnlyAnUnappliedPendingDelivery() {
    var assignments = mock(DeliveryAssignmentJpaRepository.class);
    var cleanups = mock(ReservationCleanupJpaRepository.class);
    var service = mock(DispatchApplicationService.class);
    var cleanup = mock(ReservationCleanupService.class);
    var fleet = mock(FleetClient.class);
    var delivery = mock(DeliveryClient.class);
    var assignment = new DeliveryAssignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);
    when(cleanups.findAll()).thenReturn(List.of());
    when(assignments.findByStatus(AssignmentStatus.ACTIVE)).thenReturn(List.of(assignment));
    when(fleet.reservation(assignment.getId())).thenReturn(new ReservationResponse(assignment.getId(),
        assignment.getDeliveryId(), assignment.getDriverId(), null, "RELEASED", Instant.now(), "EXPIRED"));
    when(delivery.getDelivery(assignment.getDeliveryId())).thenReturn(new DeliveryResponse(
        assignment.getDeliveryId(), UUID.randomUUID(), null, "P", "D", RemoteDeliveryStatus.PENDING));
    new DispatchReservationMaintenance(assignments, cleanups, service, cleanup, fleet, delivery).maintain();
    verify(service).rejectAssignment(eq(assignment.getId()), eq(assignment.getDeliveryId()), anyString(), any());
  }
}
