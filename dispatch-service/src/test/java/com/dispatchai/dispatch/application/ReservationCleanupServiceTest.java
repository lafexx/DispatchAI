package com.dispatchai.dispatch.application;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.dispatchai.dispatch.infrastructure.client.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationRequest;
import com.dispatchai.dispatch.infrastructure.persistence.repository.ReservationCleanupJpaRepository;

class ReservationCleanupServiceTest {
  @Test
  void failedCleanupKeepsIntentAndSuccessfulRetryRemovesIt() {
    var fleet = mock(FleetClient.class);
    var repository = mock(ReservationCleanupJpaRepository.class);
    var service = new ReservationCleanupService(fleet, repository);
    var id = UUID.randomUUID();
    var request = new ReservationRequest(UUID.randomUUID(), UUID.randomUUID(), null);
    doThrow(new IllegalStateException("Fleet unavailable")).when(fleet).release(id, request);
    service.compensate(id, request);
    verify(repository).saveAndFlush(any());
    verify(repository, never()).deleteById(id);
    doNothing().when(fleet).release(id, request);
    service.retry(id, request);
    verify(repository).deleteById(id);
  }
}
