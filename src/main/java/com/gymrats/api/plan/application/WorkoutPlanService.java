package com.gymrats.api.plan.application;

import com.gymrats.api.exercise.application.ExerciseService;
import com.gymrats.api.plan.api.PlannedExerciseRequest;
import com.gymrats.api.plan.api.WorkoutPlanPageResponse;
import com.gymrats.api.plan.api.WorkoutPlanRequest;
import com.gymrats.api.plan.api.WorkoutPlanResponse;
import com.gymrats.api.plan.domain.WorkoutPlan;
import com.gymrats.api.plan.domain.WorkoutPlanExercise;
import com.gymrats.api.plan.domain.WorkoutPlanRepository;
import com.gymrats.api.plan.domain.WorkoutPlanStatus;
import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.shared.application.ResourceNotFoundException;
import com.gymrats.api.user.domain.Modality;
import com.gymrats.api.user.domain.User;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutPlanService {
  private final WorkoutPlanRepository plans;
  private final ExerciseService exercises;

  public WorkoutPlanService(WorkoutPlanRepository plans, ExerciseService exercises) {
    this.plans = plans;
    this.exercises = exercises;
  }

  @Transactional(readOnly = true)
  public WorkoutPlanPageResponse list(User user, WorkoutPlanStatus status, Modality modality) {
    var items = modality == null
        ? plans.findByOwnerIdAndCurrentVersionTrueAndStatusOrderByUpdatedAtDesc(user.getId(), status)
        : plans.findByOwnerIdAndCurrentVersionTrueAndStatusAndModalityOrderByUpdatedAtDesc(
            user.getId(), status, modality);
    var responses = items.stream().map(WorkoutPlanResponse::from).toList();
    return new WorkoutPlanPageResponse(responses, responses.size());
  }

  @Transactional(readOnly = true)
  public WorkoutPlanResponse get(User user, UUID id) {
    return WorkoutPlanResponse.from(requireOwned(user, id));
  }

  @Transactional
  public WorkoutPlanResponse create(User user, WorkoutPlanRequest request) {
    var plan = new WorkoutPlan(user, request.name(), request.modality());
    populate(user, plan, request);
    return WorkoutPlanResponse.from(plans.save(plan));
  }

  @Transactional
  public WorkoutPlanResponse version(User user, UUID id, WorkoutPlanRequest request) {
    var previous = requireOwned(user, id);
    if (!previous.isCurrentVersion()) {
      throw new DomainConflictException("Somente a versão atual do plano pode ser editada.");
    }
    previous.supersede();
    plans.saveAndFlush(previous);
    var next = new WorkoutPlan(previous, request.name(), request.modality());
    populate(user, next, request);
    return WorkoutPlanResponse.from(plans.save(next));
  }

  @Transactional
  public WorkoutPlanResponse duplicate(User user, UUID id) {
    var source = requireOwned(user, id);
    var copy = new WorkoutPlan(user, source.getName() + " (cópia)", source.getModality());
    source.getExercises().forEach(item -> copy.addExercise(
        new WorkoutPlanExercise(
            copy,
            item.getExercise(),
            item.getPosition(),
            item.getSets(),
            item.getRepetitions(),
            item.getDurationMinutes(),
            item.getDistanceMeters())));
    return WorkoutPlanResponse.from(plans.save(copy));
  }

  @Transactional
  public WorkoutPlanResponse changeStatus(User user, UUID id, WorkoutPlanStatus status) {
    var plan = requireOwned(user, id);
    if (!plan.isCurrentVersion()) {
      throw new DomainConflictException("Somente a versão atual pode mudar de status.");
    }
    if (status == WorkoutPlanStatus.ACTIVE) plan.restore(); else plan.archive();
    return WorkoutPlanResponse.from(plan);
  }

  @Transactional(readOnly = true)
  public WorkoutPlan requireOwned(User user, UUID id) {
    return plans.findByIdAndOwnerId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado."));
  }

  private void populate(User user, WorkoutPlan plan, WorkoutPlanRequest request) {
    if (new HashSet<>(request.exerciseIds()).size() != request.exerciseIds().size()) {
      throw new DomainConflictException("Um exercício não pode aparecer duas vezes no mesmo plano.");
    }
    Map<UUID, PlannedExerciseRequest> parameters = new HashMap<>();
    if (request.plannedParameters() != null) {
      request.plannedParameters().forEach(item -> parameters.put(item.exerciseId(), item));
      if (!request.exerciseIds().containsAll(parameters.keySet())) {
        throw new DomainConflictException("Os parâmetros devem pertencer aos exercícios do plano.");
      }
    }
    for (int position = 0; position < request.exerciseIds().size(); position++) {
      var exercise = exercises.requireVisible(user, request.exerciseIds().get(position));
      if (exercise.getModality() != request.modality()) {
        throw new DomainConflictException("Todos os exercícios devem usar a modalidade do plano.");
      }
      var parameter = parameters.get(exercise.getId());
      plan.addExercise(new WorkoutPlanExercise(
          plan,
          exercise,
          position,
          parameter == null ? null : parameter.sets(),
          parameter == null ? null : parameter.repetitions(),
          parameter == null ? null : parameter.durationMinutes(),
          parameter == null ? null : parameter.distanceMeters()));
    }
  }
}
