package com.gymrats.api.social.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_members")
public class GroupMember extends AuditableEntity {
  @Id private UUID id;
  @ManyToOne(optional = false) @JoinColumn(name = "group_id") private SocialGroup group;
  @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Role role;
  @Column(name = "joined_at", nullable = false) private Instant joinedAt;

  protected GroupMember() {}

  public GroupMember(SocialGroup group, User user, Role role) {
    this.id = UUID.randomUUID();
    this.group = group;
    this.user = user;
    this.role = role;
    this.joinedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public SocialGroup getGroup() { return group; }
  public User getUser() { return user; }
  public Role getRole() { return role; }
  public Instant getJoinedAt() { return joinedAt; }
  public void changeRole(Role role) { this.role = role; }
  public enum Role { ADMIN, MEMBER }
}
