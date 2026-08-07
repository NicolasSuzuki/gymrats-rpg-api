package com.gymrats.api.progression.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdvancedProgressionResponse(
    String activeClass,
    List<ClassMasteryResponse> classMasteries,
    List<ClassMasteryEventResponse> masteryHistory,
    boolean advancedEligible,
    String currentRank,
    String nextRank,
    int consistentWeeks,
    int requiredConsistentWeeks,
    int nextRankProgress,
    Instant protectedUntil,
    boolean evaluationPaused,
    Instant pauseStartedAt,
    String pauseReason,
    int nationalEvaluationDays,
    int nationalEvaluationRequiredDays,
    String tieBreaker,
    List<AdvancedRankEventResponse> rankHistory) {

  public record ClassMasteryResponse(
      String characterClass,
      long totalXp,
      int level,
      long currentLevelXp,
      long nextLevelXp,
      boolean active) {}

  public record ClassMasteryEventResponse(
      UUID id,
      String characterClass,
      int amount,
      Instant occurredAt) {}

  public record AdvancedRankEventResponse(
      UUID id,
      String previousRank,
      String newRank,
      String reason,
      Instant occurredAt) {}
}
