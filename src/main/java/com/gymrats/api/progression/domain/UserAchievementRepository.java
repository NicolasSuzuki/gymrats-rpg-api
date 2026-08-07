package com.gymrats.api.progression.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {
  List<UserAchievement> findByUserIdOrderByUnlockedAtDesc(UUID userId);
  Optional<UserAchievement> findByUserIdAndCode(UUID userId, String code);
  boolean existsByUserIdAndCode(UUID userId, String code);
}
