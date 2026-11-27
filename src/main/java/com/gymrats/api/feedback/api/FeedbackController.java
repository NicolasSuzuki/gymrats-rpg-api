package com.gymrats.api.feedback.api;

import com.gymrats.api.feedback.application.FeedbackService;
import com.gymrats.api.user.application.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {
  public enum Category { BUG, SUGGESTION, OTHER }
  public record Request(
      @NotNull Category category,
      @NotBlank @Size(min = 10, max = 2000) String description,
      @Size(max = 500) @Pattern(regexp = "/(?!/)[^\\r\\n\\\\]*", message = "Informe uma rota local válida.") String path,
      @Pattern(regexp = "[A-Za-z0-9._-]{1,80}", message = "Identificador inválido.") String requestId) {
    public Request { if (description != null) description = description.trim(); }
  }
  public record Receipt(UUID id, Instant createdAt) {}

  private final FeedbackService service;
  private final CurrentUserService currentUser;
  public FeedbackController(FeedbackService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Receipt create(@Valid @RequestBody Request request, Authentication authentication) {
    return service.create(currentUser.require(authentication), request);
  }
}
