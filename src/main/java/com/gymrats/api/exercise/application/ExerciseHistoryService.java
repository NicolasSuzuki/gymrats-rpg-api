package com.gymrats.api.exercise.application;

import com.gymrats.api.exercise.api.ExerciseHistoryResponse;
import com.gymrats.api.exercise.api.ExerciseHistoryResponse.Entry;
import com.gymrats.api.exercise.api.ExerciseHistoryResponse.SetEntry;
import com.gymrats.api.exercise.api.ExerciseHistoryResponse.Summary;
import com.gymrats.api.user.domain.User;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseHistoryService {
  private final ExerciseService exercises;
  private final NamedParameterJdbcTemplate jdbc;
  public ExerciseHistoryService(ExerciseService exercises, NamedParameterJdbcTemplate jdbc) {
    this.exercises = exercises;
    this.jdbc = jdbc;
  }

  @Transactional(readOnly = true)
  public ExerciseHistoryResponse history(User user, UUID id) {
    var exercise = exercises.requireVisible(user, id);
    var params = Map.of("owner", user.getId(), "exercise", id);
    // Flat projections keep query count constant, regardless of the number of sessions/sets.
    var summary = jdbc.queryForObject("""
        SELECT COUNT(DISTINCT w.id) AS session_count, COUNT(s.id) AS total_sets,
          COALESCE(SUM(s.load_kg * s.repetitions), 0) AS volume,
          MAX(s.load_kg) AS max_load, MAX(s.repetitions) AS max_reps,
          MAX(s.distance_meters) AS max_distance, MAX(s.duration_seconds) AS max_duration
        FROM workout_sessions w
        JOIN workout_session_exercises e ON e.session_id = w.id
        LEFT JOIN workout_session_sets s ON s.session_exercise_id = e.id
        WHERE w.owner_id = :owner AND w.status = 'COMPLETED'
          AND e.exercise_id = :exercise AND e.status = 'COMPLETED'
        """, params, (rs, row) -> new Summary(rs.getLong("session_count"),
        rs.getLong("total_sets"), rs.getBigDecimal("volume"), rs.getBigDecimal("max_load"),
        rs.getObject("max_reps", Integer.class), rs.getObject("max_distance", Integer.class),
        rs.getObject("max_duration", Integer.class)));
    var entries = new LinkedHashMap<UUID, MutableEntry>();
    jdbc.query("""
        SELECT recent.session_id, recent.completed_at, s.id AS set_id,
          s.load_kg, s.repetitions, s.duration_seconds, s.distance_meters, s.rest_seconds, s.rpe
        FROM (
          SELECT e.id, w.id AS session_id, w.completed_at
          FROM workout_sessions w JOIN workout_session_exercises e ON e.session_id = w.id
          WHERE w.owner_id = :owner AND w.status = 'COMPLETED'
            AND e.exercise_id = :exercise AND e.status = 'COMPLETED'
          ORDER BY w.completed_at DESC, w.id DESC LIMIT 30
        ) recent
        LEFT JOIN workout_session_sets s ON s.session_exercise_id = recent.id
        ORDER BY recent.completed_at DESC, recent.session_id DESC, s.position
        """, params, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
      UUID sessionId = rs.getObject("session_id", UUID.class);
      Instant completedAt = rs.getObject("completed_at", OffsetDateTime.class).toInstant();
      var entry = entries.computeIfAbsent(sessionId, ignored -> new MutableEntry(completedAt));
      UUID setId = rs.getObject("set_id", UUID.class);
      if (setId != null) entry.sets.add(new SetEntry(setId, rs.getBigDecimal("load_kg"),
          rs.getObject("repetitions", Integer.class), rs.getObject("duration_seconds", Integer.class),
          rs.getObject("distance_meters", Integer.class), rs.getObject("rest_seconds", Integer.class),
          rs.getObject("rpe", Integer.class)));
    });
    return new ExerciseHistoryResponse(id, exercise.getName(), entries.entrySet().stream()
        .map(entry -> new Entry(entry.getKey(), entry.getValue().completedAt,
            List.copyOf(entry.getValue().sets))).toList(), summary);
  }
  private static class MutableEntry {
    final Instant completedAt;
    final List<SetEntry> sets = new ArrayList<>();
    MutableEntry(Instant completedAt) { this.completedAt = completedAt; }
  }
}
