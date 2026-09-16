package com.dispatchai.customer.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notifications", uniqueConstraints = {
    @UniqueConstraint(name = "uk_notification_source_event", columnNames = "source_event_id")
})
public class Notification {

  @Id
  private UUID id;

  @Column(name = "source_event_id", nullable = false)
  private UUID sourceEventId;

  @Column(nullable = false)
  private UUID customerId;

  @Column(nullable = false)
  private UUID deliveryId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType type;

  @Column(nullable = false)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant sentAt;

  protected Notification() {
  }

  public Notification(
      UUID id,
      UUID sourceEventId,
      UUID customerId,
      UUID deliveryId,
      NotificationType type,
      String message) {

    if (id == null) {
      throw new IllegalArgumentException("Notification ID cannot be null");
    }

    if (sourceEventId == null) {
      throw new IllegalArgumentException("Source event ID cannot be null");
    }

    if (customerId == null) {
      throw new IllegalArgumentException("Customer ID cannot be null");
    }

    if (deliveryId == null) {
      throw new IllegalArgumentException("Delivery ID cannot be null");
    }

    if (type == null) {
      throw new IllegalArgumentException("Notification type cannot be null");
    }

    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Notification message cannot be blank");
    }

    this.id = id;
    this.sourceEventId = sourceEventId;
    this.customerId = customerId;
    this.deliveryId = deliveryId;
    this.type = type;
    this.message = message;
    this.status = NotificationStatus.PENDING;
    this.createdAt = Instant.now();
  }

  public void markSent() {
    this.status = NotificationStatus.SENT;
    this.sentAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getSourceEventId() {
    return sourceEventId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getDeliveryId() {
    return deliveryId;
  }

  public NotificationType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }
}
