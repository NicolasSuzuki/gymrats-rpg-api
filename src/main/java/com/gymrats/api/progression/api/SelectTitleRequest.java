package com.gymrats.api.progression.api;

import jakarta.validation.constraints.Size;

public record SelectTitleRequest(@Size(max = 80) String title) {}
