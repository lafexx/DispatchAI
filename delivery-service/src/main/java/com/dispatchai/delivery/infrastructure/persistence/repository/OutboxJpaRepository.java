package com.dispatchai.delivery.infrastructure.persistence.repository;

import com.dispatchai.delivery.infrastructure.persistence.entity.OutboxMessage;

import java.time.Instant;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OutboxJpaRepository extends JpaRepository<OutboxMessage, Long> {
  Optional<OutboxMessage> findByEventId(UUID eventId);
  @Query("select e.id from OutboxMessage e where e.publishedAt is null and e.nextAttemptAt <= :now "
      + "and not exists (select p.id from OutboxMessage p where p.aggregateId = e.aggregateId "
      + "and p.id < e.id and p.publishedAt is null) order by e.id")
  List<Long> ready(@Param("now") Instant now, Pageable page);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from OutboxMessage e where e.id = :id")
  Optional<OutboxMessage> lock(@Param("id") Long id);
  boolean existsByAggregateIdAndIdLessThanAndPublishedAtIsNull(UUID aggregateId, Long id);
  long countByPublishedAtIsNull();
  long countByPublishedAtIsNotNull();
  List<OutboxMessage> findTop50ByPublishedAtIsNullOrderByIdAsc();
  List<OutboxMessage> findTop50ByAggregateIdAndPublishedAtIsNullOrderByIdAsc(UUID aggregateId);
}
