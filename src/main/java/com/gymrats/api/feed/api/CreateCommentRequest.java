package com.gymrats.api.feed.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @NotBlank(message = "O comentário é obrigatório.")
    @Size(max = 500, message = "O comentário deve ter até 500 caracteres.")
    String content) {}
