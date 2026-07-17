package com.gymrats.api.user.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends AuditableEntity {
  @Id private UUID id;
  @Column(nullable = false, length = 100) private String name;
  @Column(nullable = false, unique = true, length = 254) private String email;
  @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;

  protected User() {}
  public User(String name, String email, String passwordHash) { this.id = UUID.randomUUID(); this.name = name; this.email = email; this.passwordHash = passwordHash; this.role = Role.USER; }
  public UUID getId() { return id; }
  public String getName() { return name; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public Role getRole() { return role; }
}
