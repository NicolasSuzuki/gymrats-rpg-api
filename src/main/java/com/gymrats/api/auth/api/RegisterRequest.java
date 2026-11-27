package com.gymrats.api.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
    @NotBlank @Size(min = 2, max = 100) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotNull @AssertTrue(message = "Aceite os Termos e a Política de Privacidade.") Boolean acceptedTerms,
    @Pattern(regexp = "2026-11-26", message = "Versão dos termos inválida.") String termsVersion,
    Boolean photoStorageConsent) {}
