package com.gymrats.api.progression.domain;

import com.gymrats.api.user.domain.CharacterClass;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassMasteryEventRepository
    extends JpaRepository<ClassMasteryEvent, UUID> {
  boolean existsByUserIdAndCharacterClassAndSourceId(
      UUID userId, CharacterClass characterClass, UUID sourceId);

  List<ClassMasteryEvent> findTop20ByUserIdOrderByOccurredAtDesc(UUID userId);

  @Query("""
      select coalesce(sum(event.amount), 0) from ClassMasteryEvent event
      where event.user.id = :userId and event.characterClass = :characterClass
      """)
  long totalForClass(
      @Param("userId") UUID userId,
      @Param("characterClass") CharacterClass characterClass);
}
