package com.gymrats.api.user.api;

import com.gymrats.api.user.domain.CharacterClass;
import com.gymrats.api.user.domain.Modality;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record OnboardingRequest(
    @NotEmpty Set<Modality> modalities,
    @NotNull @Min(1) @Max(7) Integer weeklyGoal,
    @NotNull CharacterClass characterClass) {}
