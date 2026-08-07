package com.gymrats.api.progression.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HallOfFameResponse(
    MonarchResponse currentMonarch,
    String calculation,
    List<HallOfFameEntryResponse> entries) {

  public record MonarchResponse(
      UUID userId,
      String displayName,
      Instant reignStartedAt,
      long reignDurationSeconds,
      boolean currentUser) {}

  public record HallOfFameEntryResponse(
      UUID userId,
      String displayName,
      int reignCount,
      long longestReignSeconds,
      long totalReignSeconds,
      Instant firstReignAt,
      Instant latestReignAt,
      boolean currentMonarch,
      boolean currentUser,
      List<ReignResponse> reigns) {}

  public record ReignResponse(
      UUID id,
      Instant startedAt,
      Instant endedAt,
      long durationSeconds,
      boolean active) {}
}
