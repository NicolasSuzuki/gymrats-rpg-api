package com.gymrats.api.user.api;

import com.gymrats.api.user.domain.CharacterClass;
import com.gymrats.api.user.domain.Modality;
import com.gymrats.api.user.domain.Universe;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateProfileRequest(
    @Size(min = 2, max = 100) String name,
    @Size(min = 1) Set<Modality> modalities,
    @Min(1) @Max(7) Integer weeklyGoal,
    CharacterClass characterClass,
    Universe universe) {}
