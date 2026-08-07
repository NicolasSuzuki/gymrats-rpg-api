package com.gymrats.api.progression.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvancedRankEventRepository
    extends JpaRepository<AdvancedRankEvent, UUID> {
  List<AdvancedRankEvent> findTop20ByUserIdOrderByOccurredAtDesc(UUID userId);
}
