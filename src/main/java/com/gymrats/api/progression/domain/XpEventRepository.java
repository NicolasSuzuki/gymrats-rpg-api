package com.gymrats.api.progression.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XpEventRepository extends JpaRepository<XpEvent, UUID> {
  Optional<XpEvent> findByUserIdAndSourceTypeAndSourceId(
      UUID userId, String sourceType, UUID sourceId);

  List<XpEvent> findByUserIdOrderByOccurredAtDesc(UUID userId);

  List<XpEvent> findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(
      Instant start, Instant end);

  @Query("select coalesce(sum(event.amount), 0) from XpEvent event where event.user.id = :userId")
  long totalForUser(@Param("userId") UUID userId);

  @Query("""
      select coalesce(sum(event.amount), 0) from XpEvent event
      where event.user.id = :userId
        and event.occurredAt >= :start and event.occurredAt < :end
      """)
  long totalForUserBetween(
      @Param("userId") UUID userId,
      @Param("start") Instant start,
      @Param("end") Instant end);
}
