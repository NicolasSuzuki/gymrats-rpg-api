package com.gymrats.api.progression.api;

import java.time.Instant;

public record AchievementResponse(
    String id,
    String name,
    String description,
    String criterion,
    Instant unlockedAt,
    String title) {}
