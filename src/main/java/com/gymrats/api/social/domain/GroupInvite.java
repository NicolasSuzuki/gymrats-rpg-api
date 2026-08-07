package com.gymrats.api.social.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_invites")
public class GroupInvite extends AuditableEntity {
  @Id private UUID id;
  @ManyToOne(optional = false) @JoinColumn(name = "group_id") private SocialGroup group;
  @ManyToOne(optional = false) @JoinColumn(name = "created_by") private User createdBy;
  @Column(nullable = false, unique = true, length = 80) private String token;
  @Column(name = "expires_at", nullable = false) private Instant expiresAt;
  @Column(name = "revoked_at") private Instant revokedAt;
  @ManyToOne @JoinColumn(name = "used_by") private User usedBy;
  @Column(name = "used_at") private Instant usedAt;

  protected GroupInvite() {}

  public GroupInvite(SocialGroup group, User createdBy, String token, Instant expiresAt) {
    this.id = UUID.randomUUID();
    this.group = group;
    this.createdBy = createdBy;
    this.token = token;
    this.expiresAt = expiresAt;
  }

  public UUID getId() { return id; }
  public SocialGroup getGroup() { return group; }
  public String getToken() { return token; }
  public Instant getExpiresAt() { return expiresAt; }
  public boolean isAvailable() {
    return revokedAt == null && usedAt == null && expiresAt.isAfter(Instant.now());
  }
  public void use(User user) { this.usedBy = user; this.usedAt = Instant.now(); }
}
