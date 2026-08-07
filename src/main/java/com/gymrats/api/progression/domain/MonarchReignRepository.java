package com.gymrats.api.progression.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonarchReignRepository extends JpaRepository<MonarchReign, UUID> {
  List<MonarchReign> findAllByOrderByStartedAtDesc();

  long countByEndedAtIsNull();
}
