package com.gymrats.api.exercise.application;

import com.gymrats.api.exercise.api.ExercisePageResponse;
import com.gymrats.api.exercise.api.ExerciseRequest;
import com.gymrats.api.exercise.api.ExerciseResponse;
import com.gymrats.api.exercise.domain.Exercise;
import com.gymrats.api.exercise.domain.ExerciseRepository;
import com.gymrats.api.exercise.domain.ExerciseVisibility;
import com.gymrats.api.shared.application.ResourceNotFoundException;
import com.gymrats.api.user.domain.Modality;
import com.gymrats.api.user.domain.Role;
import com.gymrats.api.user.domain.User;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseService {
  private final ExerciseRepository exercises;

  public ExerciseService(ExerciseRepository exercises) { this.exercises = exercises; }

  @Transactional(readOnly = true)
  public ExercisePageResponse search(
      User user,
      int page,
      int pageSize,
      String name,
      Modality modality,
      String category,
      String muscleGroup) {
    var result = exercises.searchVisible(
        user.getId(),
        normalize(name),
        modality,
        normalize(category),
        normalize(muscleGroup),
        PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(pageSize, 1), 100)));
    return new ExercisePageResponse(
        result.getContent().stream().map(ExerciseResponse::from).toList(),
        result.getNumber() + 1,
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public ExerciseResponse get(User user, UUID id) {
    return ExerciseResponse.from(requireVisible(user, id));
  }

  @Transactional
  public ExerciseResponse createGlobal(ExerciseRequest request) {
    return ExerciseResponse.from(save(new Exercise(null, ExerciseVisibility.GLOBAL), request));
  }

  @Transactional
  public ExerciseResponse createPersonal(User user, ExerciseRequest request) {
    return ExerciseResponse.from(save(new Exercise(user, ExerciseVisibility.PERSONAL), request));
  }

  @Transactional
  public ExerciseResponse update(User user, UUID id, ExerciseRequest request) {
    var exercise = requireVisible(user, id);
    authorizeMutation(user, exercise);
    return ExerciseResponse.from(save(exercise, request));
  }

  @Transactional
  public void archive(User user, UUID id) {
    var exercise = requireVisible(user, id);
    authorizeMutation(user, exercise);
    exercise.archive();
  }

  @Transactional(readOnly = true)
  public Exercise requireVisible(User user, UUID id) {
    var exercise = exercises.findByIdAndArchivedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException("Exercício não encontrado."));
    if (exercise.getVisibility() == ExerciseVisibility.PERSONAL
        && !exercise.getOwner().getId().equals(user.getId())) {
      throw new ResourceNotFoundException("Exercício não encontrado.");
    }
    return exercise;
  }

  private Exercise save(Exercise exercise, ExerciseRequest request) {
    exercise.update(
        request.name(),
        request.modality(),
        request.category(),
        request.muscleGroup(),
        request.description(),
        request.instructions(),
        request.equipment(),
        request.difficulty(),
        request.terrain(),
        request.stroke(),
        request.poolLengthMeters());
    return exercises.save(exercise);
  }

  private void authorizeMutation(User user, Exercise exercise) {
    var ownsPersonal = exercise.getVisibility() == ExerciseVisibility.PERSONAL
        && exercise.getOwner().getId().equals(user.getId());
    if (!ownsPersonal && user.getRole() != Role.ADMIN) {
      throw new AccessDeniedException("Sem permissão para alterar este exercício.");
    }
  }

  private String normalize(String value) { return value == null ? "" : value.trim(); }
}
