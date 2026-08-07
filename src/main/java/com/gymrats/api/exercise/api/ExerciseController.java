package com.gymrats.api.exercise.api;

import com.gymrats.api.exercise.application.ExerciseService;
import com.gymrats.api.user.application.CurrentUserService;
import com.gymrats.api.user.domain.Modality;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exercises")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseController {
  private final ExerciseService service;
  private final CurrentUserService currentUser;

  public ExerciseController(ExerciseService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping
  @Operation(summary = "Lista exercícios globais e pessoais visíveis")
  public ExercisePageResponse search(
      Authentication authentication,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "") String name,
      @RequestParam(required = false) Modality modality,
      @RequestParam(defaultValue = "") String category,
      @RequestParam(defaultValue = "") String muscleGroup) {
    return service.search(
        currentUser.require(authentication), page, pageSize, name, modality, category, muscleGroup);
  }

  @GetMapping("/{id}")
  public ExerciseResponse get(@PathVariable UUID id, Authentication authentication) {
    return service.get(currentUser.require(authentication), id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public ExerciseResponse createGlobal(@Valid @RequestBody ExerciseRequest request) {
    return service.createGlobal(request);
  }

  @PostMapping("/personal")
  @ResponseStatus(HttpStatus.CREATED)
  public ExerciseResponse createPersonal(
      @Valid @RequestBody ExerciseRequest request, Authentication authentication) {
    return service.createPersonal(currentUser.require(authentication), request);
  }

  @PutMapping("/{id}")
  public ExerciseResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody ExerciseRequest request,
      Authentication authentication) {
    return service.update(currentUser.require(authentication), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archive(@PathVariable UUID id, Authentication authentication) {
    service.archive(currentUser.require(authentication), id);
  }
}
