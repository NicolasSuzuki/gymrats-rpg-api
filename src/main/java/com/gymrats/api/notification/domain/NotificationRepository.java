package com.gymrats.api.notification.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
  Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
  Optional<Notification> findByUserIdAndEventId(UUID userId, String eventId);
  long countByUserIdAndReadAtIsNull(UUID userId);
  java.util.List<Notification> findByUserIdAndReadAtIsNull(UUID userId);
}
