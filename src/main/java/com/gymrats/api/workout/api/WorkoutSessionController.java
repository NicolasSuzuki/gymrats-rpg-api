package com.gymrats.api.workout.api;

import com.gymrats.api.user.application.CurrentUserService;
import com.gymrats.api.workout.application.WorkoutSessionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workout-sessions")
@SecurityRequirement(name = "bearerAuth")
public class WorkoutSessionController {
  private final WorkoutSessionService service;
  private final CurrentUserService currentUser;

  public WorkoutSessionController(
      WorkoutSessionService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WorkoutSessionResponse start(
      @RequestBody StartWorkoutSessionRequest request, Authentication authentication) {
    return service.start(currentUser.require(authentication), request);
  }

  @GetMapping("/active")
  public WorkoutSessionResponse active(Authentication authentication) {
    return service.active(currentUser.require(authentication));
  }

  @PatchMapping("/{id}/exercises/{exerciseId}")
  public SessionExerciseResponse updateExercise(
      @PathVariable UUID id,
      @PathVariable UUID exerciseId,
      @Valid @RequestBody UpdateSessionExerciseRequest request,
      Authentication authentication) {
    return service.updateExercise(
        currentUser.require(authentication), id, exerciseId, request).exercise();
  }

  @PostMapping("/{id}/complete")
  public WorkoutSessionResponse complete(
      @PathVariable UUID id,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.complete(currentUser.require(authentication), id, idempotencyKey);
  }

  @PostMapping("/{id}/cancel")
  public WorkoutSessionResponse cancel(
      @PathVariable UUID id,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.cancel(currentUser.require(authentication), id, idempotencyKey);
  }
}
