package com.dispatchai.dispatch.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dispatchai.dispatch.domain.model.ReservationCleanup;

public interface ReservationCleanupJpaRepository extends JpaRepository<ReservationCleanup, UUID> {
}
