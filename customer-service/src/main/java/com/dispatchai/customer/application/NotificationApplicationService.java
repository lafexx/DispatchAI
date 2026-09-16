package com.dispatchai.customer.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dispatchai.customer.domain.exceptions.CustomerNotFoundException;
import com.dispatchai.customer.domain.model.Notification;
import com.dispatchai.customer.domain.model.NotificationType;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryCompletedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryStartedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DriverAssignedEvent;
import com.dispatchai.customer.infrastructure.persistence.CustomerJpaRepository;
import com.dispatchai.customer.infrastructure.persistence.NotificationJpaRepository;
import com.dispatchai.customer.presentation.dto.NotificationResponse;

@Service
public class NotificationApplicationService {

  private final CustomerJpaRepository customerRepository;
  private final NotificationJpaRepository notificationRepository;

  public NotificationApplicationService(
      CustomerJpaRepository customerRepository,
      NotificationJpaRepository notificationRepository) {

    this.customerRepository = customerRepository;
    this.notificationRepository = notificationRepository;
  }

  public List<NotificationResponse> getForCustomer(UUID id) {
    var notifications = notificationRepository.findByCustomerIdOrderByCreatedAtDesc(id);

    return toResponse(notifications);
  }

  public void notifyDriverAssigned(
      DriverAssignedEvent event) {

    createNotification(
        event.eventId(),
        event.customerId(),
        event.deliveryId(),
        NotificationType.DRIVER_ASSIGNED,
        "A driver has been assigned to your delivery.");
  }

  public void notifyDeliveryStarted(
      DeliveryStartedEvent event) {

    createNotification(
        event.eventId(),
        event.customerId(),
        event.deliveryId(),
        NotificationType.DELIVERY_STARTED,
        "Your delivery is now in transit.");
  }

  public void notifyDeliveryCompleted(
      DeliveryCompletedEvent event) {

    createNotification(
        event.eventId(),
        event.customerId(),
        event.deliveryId(),
        NotificationType.DELIVERY_COMPLETED,
        "Your delivery has been completed.");
  }

  public void notifyDeliveryCancelled(
      DeliveryCancelledEvent event) {

    createNotification(
        event.eventId(),
        event.customerId(),
        event.deliveryId(),
        NotificationType.DELIVERY_CANCELLED,
        "Your delivery has been cancelled: " + event.reason());
  }

  private void createNotification(
      UUID eventId,
      UUID customerId,
      UUID deliveryId,
      NotificationType type,
      String message) {

    if (notificationRepository.existsBySourceEventId(eventId)) {
      return;
    }

    customerRepository.findById(customerId)
        .orElseThrow(() -> new CustomerNotFoundException(customerId));

    var notification = new Notification(
        UUID.randomUUID(),
        eventId,
        customerId,
        deliveryId,
        type,
        message);

    notificationRepository.save(notification);

    // NOTE: notification delivery is simulated for now. AWS SNS, twilio, etc. could
    // be used later on
    notification.markSent();

    notificationRepository.save(notification);
  }

  private List<NotificationResponse> toResponse(List<Notification> notifications) {
    List<NotificationResponse> responses = new ArrayList<NotificationResponse>();

    for (Notification notification : notifications) {
      responses.add(new NotificationResponse(
          notification.getId(),
          notification.getSourceEventId(),
          notification.getCustomerId(),
          notification.getDeliveryId(),
          notification.getType(),
          notification.getMessage(),
          notification.getStatus(),
          notification.getCreatedAt()));
    }

    return responses;
  }
}
