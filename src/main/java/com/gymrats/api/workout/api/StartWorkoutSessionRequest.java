package com.gymrats.api.workout.api;

import com.gymrats.api.workout.domain.WorkoutSessionSource;
import java.util.List;
import java.util.UUID;

public record StartWorkoutSessionRequest(
    UUID planId, WorkoutSessionSource type, List<UUID> exerciseIds) {}
