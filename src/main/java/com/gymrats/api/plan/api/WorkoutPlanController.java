package com.gymrats.api.plan.api;

import com.gymrats.api.plan.application.WorkoutPlanService;
import com.gymrats.api.plan.domain.WorkoutPlanStatus;
import com.gymrats.api.user.application.CurrentUserService;
import com.gymrats.api.user.domain.Modality;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/plans")
@SecurityRequirement(name = "bearerAuth")
public class WorkoutPlanController {
  private final WorkoutPlanService service;
  private final CurrentUserService currentUser;

  public WorkoutPlanController(WorkoutPlanService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping
  public WorkoutPlanPageResponse list(
      Authentication authentication,
      @RequestParam(defaultValue = "ACTIVE") WorkoutPlanStatus status,
      @RequestParam(required = false) Modality modality) {
    return service.list(currentUser.require(authentication), status, modality);
  }

  @GetMapping("/{id}")
  public WorkoutPlanResponse get(@PathVariable UUID id, Authentication authentication) {
    return service.get(currentUser.require(authentication), id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WorkoutPlanResponse create(
      @Valid @RequestBody WorkoutPlanRequest request, Authentication authentication) {
    return service.create(currentUser.require(authentication), request);
  }

  @PutMapping("/{id}")
  public WorkoutPlanResponse version(
      @PathVariable UUID id,
      @Valid @RequestBody WorkoutPlanRequest request,
      Authentication authentication) {
    return service.version(currentUser.require(authentication), id, request);
  }

  @PostMapping("/{id}/duplicate")
  @ResponseStatus(HttpStatus.CREATED)
  public WorkoutPlanResponse duplicate(@PathVariable UUID id, Authentication authentication) {
    return service.duplicate(currentUser.require(authentication), id);
  }

  @PostMapping("/{id}/archive")
  public WorkoutPlanResponse archive(@PathVariable UUID id, Authentication authentication) {
    return service.changeStatus(
        currentUser.require(authentication), id, WorkoutPlanStatus.ARCHIVED);
  }

  @PostMapping("/{id}/restore")
  public WorkoutPlanResponse restore(@PathVariable UUID id, Authentication authentication) {
    return service.changeStatus(currentUser.require(authentication), id, WorkoutPlanStatus.ACTIVE);
  }
}
