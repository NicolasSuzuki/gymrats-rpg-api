package com.gymrats.api.user.api;

import com.gymrats.api.user.domain.User;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    String role,
    boolean active,
    boolean onboardingCompleted,
    Set<String> modalities,
    Integer weeklyGoal,
    String characterClass,
    String universe,
    Instant createdAt,
    Instant updatedAt,
    boolean photoStorageConsent,
    Instant termsAcceptedAt,
    String termsVersion) {
  public static UserResponse from(User user, Integer weeklyGoal) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getRole().name(),
        user.isActive(),
        user.isOnboardingCompleted(),
        user.getModalities().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()),
        weeklyGoal,
        user.getCharacterClass() == null ? null : user.getCharacterClass().name(),
        user.getUniverse().name().toLowerCase(java.util.Locale.ROOT),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        user.isPhotoStorageConsent(),
        user.getTermsAcceptedAt(),
        user.getTermsVersion());
  }
}
