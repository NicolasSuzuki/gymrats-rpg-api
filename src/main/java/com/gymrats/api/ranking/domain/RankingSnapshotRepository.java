package com.gymrats.api.ranking.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingSnapshotRepository extends JpaRepository<RankingSnapshot, UUID> {
  Optional<RankingSnapshot> findBySnapshotKeyAndUserId(String snapshotKey, UUID userId);
}
