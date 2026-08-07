package com.gymrats.api.progression.api;

import java.time.Instant;
import java.util.UUID;

public record ProgressionEventResponse(
    UUID id,
    String type,
    String description,
    String source,
    Instant occurredAt) {}
