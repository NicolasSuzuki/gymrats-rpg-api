package com.gymrats.api.progression.api;

public record ProgressionOverviewResponse(
    long totalXp,
    int level,
    long currentLevelXp,
    long nextLevelXp,
    String formulaDescription,
    int weeklyTarget,
    long weeklyCompleted,
    String rank,
    String nextRank,
    int nextRankProgress) {}
