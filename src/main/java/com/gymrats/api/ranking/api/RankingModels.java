package com.gymrats.api.ranking.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RankingModels {
  private RankingModels() {}
  public record Entry(
      UUID userId, String displayName, int position, int value, boolean isCurrentUser) {}
  public record Result(
      String period,
      String metric,
      String scope,
      Instant periodStart,
      Instant periodEnd,
      String calculation,
      List<Entry> entries,
      int page,
      int totalPages) {}
}
