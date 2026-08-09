package com.gymrats.api.workout.application;

import com.gymrats.api.exercise.application.ExerciseService;
import com.gymrats.api.plan.application.WorkoutPlanService;
import com.gymrats.api.plan.domain.WorkoutPlanStatus;
import com.gymrats.api.progression.application.ProgressionService;
import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.shared.application.ResourceNotFoundException;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.workout.api.StartWorkoutSessionRequest;
import com.gymrats.api.workout.api.UpdateSessionExerciseRequest;
import com.gymrats.api.workout.api.WorkoutSessionResponse;
import com.gymrats.api.workout.domain.IdempotencyOperation;
import com.gymrats.api.workout.domain.IdempotencyOperationRepository;
import com.gymrats.api.workout.domain.WorkoutSession;
import com.gymrats.api.workout.domain.WorkoutSessionExercise;
import com.gymrats.api.workout.domain.WorkoutSessionRepository;
import com.gymrats.api.workout.domain.WorkoutSessionSet;
import com.gymrats.api.workout.domain.WorkoutSessionSource;
import com.gymrats.api.workout.domain.WorkoutSessionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutSessionService {
  private final WorkoutSessionRepository sessions;
  private final IdempotencyOperationRepository operations;
  private final WorkoutPlanService plans;
  private final ExerciseService exercises;
  private final ProgressionService progression;

  public WorkoutSessionService(
      WorkoutSessionRepository sessions,
      IdempotencyOperationRepository operations,
      WorkoutPlanService plans,
      ExerciseService exercises,
      ProgressionService progression) {
    this.sessions = sessions;
    this.operations = operations;
    this.plans = plans;
    this.exercises = exercises;
    this.progression = progression;
  }

  @Transactional
  public WorkoutSessionResponse start(User user, StartWorkoutSessionRequest request) {
    sessions.findByOwnerIdAndStatus(user.getId(), WorkoutSessionStatus.IN_PROGRESS)
        .ifPresent(existing -> {
          throw new DomainConflictException("Você já possui uma sessão em andamento.");
        });
    if (request.planId() != null && request.type() != null) {
      throw new DomainConflictException("Informe um plano ou um treino livre, não ambos.");
    }
    WorkoutSession session;
    if (request.planId() != null) {
      var plan = plans.requireOwned(user, request.planId());
      if (!plan.isCurrentVersion() || plan.getStatus() != WorkoutPlanStatus.ACTIVE) {
        throw new ResourceNotFoundException("Plano ativo não encontrado.");
      }
      session = new WorkoutSession(user, WorkoutSessionSource.PLAN, plan);
      for (var item : plan.getExercises()) {
        session.addExercise(new WorkoutSessionExercise(
            session, item.getExercise().getId(), item.getPosition()));
      }
    } else {
      if (request.type() != WorkoutSessionSource.FREE) {
        throw new DomainConflictException("Informe planId ou type FREE.");
      }
      session = new WorkoutSession(user, WorkoutSessionSource.FREE, null);
      var exerciseIds = request.exerciseIds() == null ? List.<UUID>of() : request.exerciseIds();
      for (int position = 0; position < exerciseIds.size(); position++) {
        var exercise = exercises.requireVisible(user, exerciseIds.get(position));
        session.addExercise(new WorkoutSessionExercise(session, exercise.getId(), position));
      }
    }
    return WorkoutSessionResponse.from(sessions.save(session));
  }

  @Transactional(readOnly = true)
  public WorkoutSessionResponse active(User user) {
    return sessions.findByOwnerIdAndStatus(user.getId(), WorkoutSessionStatus.IN_PROGRESS)
        .map(WorkoutSessionResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException("Nenhuma sessão em andamento."));
  }

  @Transactional
  public WorkoutSessionResponse quickCheckIn(User user, String key) {
    validateIdempotencyKey(key);
    var replay = operations.findByOwnerIdAndKey(user.getId(), key);
    if (replay.isPresent()) {
      var saved = replay.get();
      if (!saved.getOperation().equals("COMPLETE")) {
        throw new DomainConflictException("Idempotency-Key já utilizada em outra operação.");
      }
      return WorkoutSessionResponse.from(saved.getSession());
    }
    sessions.findByOwnerIdAndStatus(user.getId(), WorkoutSessionStatus.IN_PROGRESS)
        .ifPresent(existing -> {
          throw new DomainConflictException(
              "Finalize ou cancele o treino em andamento antes do check-in rápido.");
        });
    var session = new WorkoutSession(user, WorkoutSessionSource.FREE, null);
    session.complete();
    sessions.save(session);
    progression.awardWorkout(user, session);
    operations.save(new IdempotencyOperation(user.getId(), key, "COMPLETE", session));
    return WorkoutSessionResponse.from(session);
  }

  @Transactional
  public SessionExerciseResult updateExercise(
      User user, UUID sessionId, UUID exerciseId, UpdateSessionExerciseRequest request) {
    if (request.status() == null && request.data() == null) {
      throw new DomainConflictException("Nenhuma alteração informada.");
    }
    var session = requireOwned(user, sessionId);
    ensureInProgress(session);
    var item = session.getExercises().stream()
        .filter(value -> value.getExerciseId().equals(exerciseId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Exercício da sessão não encontrado."));
    item.updateStatus(request.status());
    if (request.data() != null) {
      var requests = request.data().sets() == null ? List.<com.gymrats.api.workout.api.SessionSetRequest>of() : request.data().sets();
      var sets = new java.util.ArrayList<WorkoutSessionSet>();
      for (int position = 0; position < requests.size(); position++) {
        var value = requests.get(position);
        sets.add(new WorkoutSessionSet(
            item,
            position,
            value.loadKg(),
            value.repetitions(),
            value.durationSeconds(),
            value.distanceMeters(),
            value.restSeconds(),
            value.rpe()));
      }
      item.replaceData(request.data().notes(), sets);
    }
    sessions.save(session);
    return new SessionExerciseResult(com.gymrats.api.workout.api.SessionExerciseResponse.from(item));
  }

  @Transactional
  public WorkoutSessionResponse complete(User user, UUID sessionId, String key) {
    return terminal(user, sessionId, key, "COMPLETE", true);
  }

  @Transactional
  public WorkoutSessionResponse cancel(User user, UUID sessionId, String key) {
    return terminal(user, sessionId, key, "CANCEL", false);
  }

  private WorkoutSessionResponse terminal(
      User user, UUID sessionId, String key, String operation, boolean complete) {
    validateIdempotencyKey(key);
    var replay = operations.findByOwnerIdAndKey(user.getId(), key);
    if (replay.isPresent()) {
      var saved = replay.get();
      if (!saved.getOperation().equals(operation)
          || !saved.getSession().getId().equals(sessionId)) {
        throw new DomainConflictException("Idempotency-Key já utilizada em outra operação.");
      }
      return WorkoutSessionResponse.from(saved.getSession());
    }
    var session = requireOwned(user, sessionId);
    ensureInProgress(session);
    if (complete) session.complete(); else session.cancel();
    sessions.save(session);
    if (complete) progression.awardWorkout(user, session);
    operations.save(new IdempotencyOperation(user.getId(), key, operation, session));
    return WorkoutSessionResponse.from(session);
  }

  private void validateIdempotencyKey(String key) {
    if (key == null || key.isBlank() || key.length() > 120) {
      throw new DomainConflictException(
          "Idempotency-Key é obrigatório e deve ter até 120 caracteres.");
    }
  }

  private WorkoutSession requireOwned(User user, UUID id) {
    return sessions.findByIdAndOwnerId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada."));
  }

  private void ensureInProgress(WorkoutSession session) {
    if (session.getStatus() != WorkoutSessionStatus.IN_PROGRESS) {
      throw new DomainConflictException("A sessão já foi finalizada.");
    }
  }

  public record SessionExerciseResult(
      com.gymrats.api.workout.api.SessionExerciseResponse exercise) {}
}
