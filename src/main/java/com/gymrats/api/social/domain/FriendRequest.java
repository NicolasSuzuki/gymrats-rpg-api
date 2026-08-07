package com.gymrats.api.social.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "friend_requests")
public class FriendRequest extends AuditableEntity {
  @Id private UUID id;
  @ManyToOne(optional = false) @JoinColumn(name = "sender_id") private User sender;
  @ManyToOne(optional = false) @JoinColumn(name = "receiver_id") private User receiver;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Status status;

  protected FriendRequest() {}

  public FriendRequest(User sender, User receiver) {
    this.id = UUID.randomUUID();
    this.sender = sender;
    this.receiver = receiver;
    this.status = Status.PENDING;
  }

  public UUID getId() { return id; }
  public User getSender() { return sender; }
  public User getReceiver() { return receiver; }
  public Status getStatus() { return status; }
  public void decide(Status status) { this.status = status; }

  public enum Status { PENDING, ACCEPTED, DECLINED }
}
