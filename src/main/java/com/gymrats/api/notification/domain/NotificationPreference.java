package com.gymrats.api.notification.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference extends AuditableEntity {
  @Id private UUID id;
  @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Notification.Kind kind;
  @Column(nullable = false) private boolean enabled;

  protected NotificationPreference() {}
  public NotificationPreference(User user, Notification.Kind kind, boolean enabled) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.kind = kind;
    this.enabled = enabled;
  }
  public Notification.Kind getKind() { return kind; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
