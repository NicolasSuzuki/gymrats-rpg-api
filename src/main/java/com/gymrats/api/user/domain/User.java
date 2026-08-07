package com.gymrats.api.user.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User extends AuditableEntity {
  @Id private UUID id;
  @Column(nullable = false, length = 100) private String name;
  @Column(nullable = false, unique = true, length = 254) private String email;
  @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
  @Column(nullable = false) private boolean active;
  @Column(name = "onboarding_completed", nullable = false) private boolean onboardingCompleted;
  @Enumerated(EnumType.STRING) @Column(name = "character_class", length = 20) private CharacterClass characterClass;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Universe universe;
  @Column(name = "selected_title", length = 80) private String selectedTitle;
  @Column(name = "ranking_visible", nullable = false) private boolean rankingVisible = true;
  @Column(name = "deleted_at") private Instant deletedAt;
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_modalities", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "modality", nullable = false, length = 20)
  private Set<Modality> modalities = new LinkedHashSet<>();

  protected User() {}
  public User(String name, String email, String passwordHash) { this.id = UUID.randomUUID(); this.name = name; this.email = email; this.passwordHash = passwordHash; this.role = Role.USER; this.active = true; this.onboardingCompleted = false; this.universe = Universe.HUNTER; }
  public UUID getId() { return id; }
  public String getName() { return name; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public Role getRole() { return role; }
  public boolean isActive() { return active; }
  public boolean isOnboardingCompleted() { return onboardingCompleted; }
  public CharacterClass getCharacterClass() { return characterClass; }
  public Universe getUniverse() { return universe; }
  public String getSelectedTitle() { return selectedTitle; }
  public boolean isRankingVisible() { return rankingVisible; }
  public Instant getDeletedAt() { return deletedAt; }
  public Set<Modality> getModalities() { return Set.copyOf(modalities); }
  public void updateProfile(String name, Set<Modality> modalities, CharacterClass characterClass, Universe universe) {
    if (name != null) this.name = name.trim();
    if (modalities != null) this.modalities = new LinkedHashSet<>(modalities);
    if (characterClass != null) this.characterClass = characterClass;
    if (universe != null) this.universe = universe;
  }
  public void completeOnboarding(Set<Modality> modalities, CharacterClass characterClass) {
    this.modalities = new LinkedHashSet<>(modalities);
    this.characterClass = characterClass;
    this.onboardingCompleted = true;
  }
  public void selectTitle(String selectedTitle) {
    this.selectedTitle = selectedTitle == null || selectedTitle.isBlank() ? null : selectedTitle.trim();
  }
  public void setRankingVisible(boolean rankingVisible) { this.rankingVisible = rankingVisible; }
  public void anonymize() {
    this.name = "Usuário excluído";
    this.email = "deleted+" + id + "@deleted.invalid";
    this.passwordHash = "ACCOUNT_DELETED";
    this.active = false;
    this.onboardingCompleted = false;
    this.characterClass = null;
    this.selectedTitle = null;
    this.rankingVisible = false;
    this.modalities.clear();
    this.deletedAt = Instant.now();
  }
  public void deactivate() { this.active = false; }
}
