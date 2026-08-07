package com.gymrats.api.exercise.api;

import com.gymrats.api.exercise.domain.Difficulty;
import com.gymrats.api.user.domain.Modality;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ExerciseRequest(
    @NotBlank @Size(max = 120) String name,
    @NotNull Modality modality,
    @NotBlank @Size(max = 80) String category,
    @Size(max = 80) String muscleGroup,
    @Size(max = 1000) String description,
    @Size(max = 20) List<@NotBlank @Size(max = 500) String> instructions,
    @Size(max = 120) String equipment,
    Difficulty difficulty,
    @Size(max = 80) String terrain,
    @Size(max = 80) String stroke,
    @Min(10) @Max(100) Integer poolLengthMeters) {}
