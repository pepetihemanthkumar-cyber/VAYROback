package com.vayro.notification.repository;

import com.vayro.notification.entity.Notification;
import com.vayro.notification.entity.NotificationStatus;
import com.vayro.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Notification> findByBookingReference(String bookingReference);

    Optional<Notification> findFirstByBookingReferenceOrderByCreatedAtDesc(String bookingReference);

    Optional<Notification> findFirstByBookingReferenceAndTypeOrderByCreatedAtDesc(String bookingReference, NotificationType type);

    List<Notification> findByRecipientEmail(String recipientEmail);

    List<Notification> findByStatus(NotificationStatus status);
}
