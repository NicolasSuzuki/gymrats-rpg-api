package com.gymrats.api.feed.domain;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutMediaRepository extends JpaRepository<WorkoutMedia, UUID> {
  Optional<WorkoutMedia> findBySessionId(UUID sessionId);
  List<WorkoutMedia> findByOwnerId(UUID ownerId);
}
