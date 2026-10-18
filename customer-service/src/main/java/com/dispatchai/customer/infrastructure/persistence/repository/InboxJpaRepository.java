package com.dispatchai.customer.infrastructure.persistence.repository;

import com.dispatchai.customer.infrastructure.persistence.entity.InboxEntry;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InboxJpaRepository extends JpaRepository<InboxEntry, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from InboxEntry e where e.id = :id")
  Optional<InboxEntry> lock(@Param("id") UUID id);
}
