package com.gymrats.api.feed.api;

import jakarta.validation.constraints.Size;

public record PublishWorkoutRequest(
    @Size(max = 280, message = "A legenda deve ter até 280 caracteres.") String caption) {}
