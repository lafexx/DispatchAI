package com.dispatchai.fleet.infrastructure.scheduling;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dispatchai.fleet.application.FleetReservationService;
import com.dispatchai.fleet.domain.model.FleetReservationStatus;
import com.dispatchai.fleet.infrastructure.persistence.repository.FleetReservationJpaRepository;

@Component
public class ReservationMaintenance {
  private static final Logger log = LoggerFactory.getLogger(ReservationMaintenance.class);
  private final FleetReservationJpaRepository repository;
  private final FleetReservationService service;
  private final Clock clock;

  public ReservationMaintenance(FleetReservationJpaRepository repository, FleetReservationService service,
      Clock clock) {
    this.repository = repository;
    this.service = service;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${fleet.reservation.sweep-ms:5000}")
  public void expireUnconfirmedHolds() {
    for (var reservation : repository.findTop50ByStatusAndExpiresAtLessThanEqualOrderByExpiresAt(
        FleetReservationStatus.RESERVED, clock.instant())) {
      try {
        service.expire(reservation.getAssignmentId(), reservation.getDeliveryId());
      } catch (RuntimeException ex) {
        log.warn("Reservation expiry failed id={}", reservation.getAssignmentId(), ex);
      }
    }
  }

  @Configuration
  public static class TimeConfiguration {
    @Bean
    public Clock fleetReservationClock() {
      return Clock.systemUTC();
    }
  }
}
