package com.gymrats.api.plan.api;

import java.util.List;

public record WorkoutPlanPageResponse(List<WorkoutPlanResponse> items, long totalItems) {}
