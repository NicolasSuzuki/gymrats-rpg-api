package com.gymrats.api.social.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "friendships")
public class Friendship extends AuditableEntity {
  @Id private UUID id;
  @ManyToOne(optional = false) @JoinColumn(name = "user_low_id") private User userLow;
  @ManyToOne(optional = false) @JoinColumn(name = "user_high_id") private User userHigh;

  protected Friendship() {}

  public Friendship(User first, User second) {
    this.id = UUID.randomUUID();
    if (first.getId().compareTo(second.getId()) < 0) {
      this.userLow = first;
      this.userHigh = second;
    } else {
      this.userLow = second;
      this.userHigh = first;
    }
  }

  public UUID getId() { return id; }
  public User getUserLow() { return userLow; }
  public User getUserHigh() { return userHigh; }
  public User other(UUID userId) {
    return userLow.getId().equals(userId) ? userHigh : userLow;
  }
}
