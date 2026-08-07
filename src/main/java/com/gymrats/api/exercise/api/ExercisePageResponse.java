package com.gymrats.api.exercise.api;

import java.util.List;

public record ExercisePageResponse(
    List<ExerciseResponse> items, int page, int pageSize, long totalItems, int totalPages) {}
