package com.dispatchai.delivery.infrastructure.persistence;

import com.dispatchai.delivery.domain.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {
}
