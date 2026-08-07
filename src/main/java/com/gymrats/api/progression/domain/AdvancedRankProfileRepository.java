package com.gymrats.api.progression.domain;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvancedRankProfileRepository
    extends JpaRepository<AdvancedRankProfile, UUID> {
  List<AdvancedRankProfile> findByCurrentRank(AdvancedRank currentRank);
}
