package com.gymrats.api.notification.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends AuditableEntity {
  @Id private UUID id;
  @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
  @Column(name = "event_id", nullable = false, length = 160) private String eventId;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Kind kind;
  @Column(nullable = false, length = 120) private String title;
  @Column(nullable = false, length = 300) private String message;
  @Column(length = 240) private String href;
  @Column(name = "read_at") private Instant readAt;

  protected Notification() {}

  public Notification(
      User user, String eventId, Kind kind, String title, String message, String href) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.eventId = eventId;
    this.kind = kind;
    this.title = title;
    this.message = message;
    this.href = href;
  }

  public UUID getId() { return id; }
  public User getUser() { return user; }
  public String getEventId() { return eventId; }
  public Kind getKind() { return kind; }
  public String getTitle() { return title; }
  public String getMessage() { return message; }
  public String getHref() { return href; }
  public Instant getReadAt() { return readAt; }
  public void markRead() { if (readAt == null) readAt = Instant.now(); }

  public enum Kind {
    COMMENT, FRIENDSHIP, GROUP, ACHIEVEMENT, PROGRESSION, RETURN
  }
}
