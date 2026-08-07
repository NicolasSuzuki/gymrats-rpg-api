package com.gymrats.api.progression.api;

import jakarta.validation.constraints.Size;

public record UpdateProgressionPauseRequest(
    boolean paused,
    @Size(max = 160) String reason) {}
