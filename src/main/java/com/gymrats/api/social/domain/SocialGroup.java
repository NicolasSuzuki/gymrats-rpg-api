package com.gymrats.api.social.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "social_groups")
public class SocialGroup extends AuditableEntity {
  @Id private UUID id;
  @Column(nullable = false, length = 80) private String name;
  @Column(length = 300) private String description;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Visibility visibility;
  @ManyToOne(optional = false) @JoinColumn(name = "admin_id") private User admin;

  protected SocialGroup() {}

  public SocialGroup(String name, String description, Visibility visibility, User admin) {
    this.id = UUID.randomUUID();
    this.name = name.trim();
    this.description = normalize(description);
    this.visibility = visibility;
    this.admin = admin;
  }

  public UUID getId() { return id; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public Visibility getVisibility() { return visibility; }
  public User getAdmin() { return admin; }
  public void update(String name, String description, Visibility visibility) {
    if (name != null && !name.isBlank()) this.name = name.trim();
    if (description != null) this.description = normalize(description);
    if (visibility != null) this.visibility = visibility;
  }
  public void transferTo(User user) { this.admin = user; }
  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public enum Visibility { PUBLIC, PRIVATE }
}
