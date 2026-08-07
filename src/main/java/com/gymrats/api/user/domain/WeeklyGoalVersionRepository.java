package com.gymrats.api.user.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyGoalVersionRepository extends JpaRepository<WeeklyGoalVersion, UUID> {
  Optional<WeeklyGoalVersion> findFirstByUserIdAndValidUntilIsNullOrderByValidFromDesc(UUID userId);
  List<WeeklyGoalVersion> findByUserIdOrderByValidFromDesc(UUID userId);
}
