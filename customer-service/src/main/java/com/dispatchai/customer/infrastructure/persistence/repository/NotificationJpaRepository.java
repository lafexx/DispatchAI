package com.dispatchai.customer.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dispatchai.customer.domain.model.Notification;

public interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findByCustomerIdOrderByCreatedAtDesc(
      UUID customerId);

  boolean existsBySourceEventId(UUID sourceEventId);
}
