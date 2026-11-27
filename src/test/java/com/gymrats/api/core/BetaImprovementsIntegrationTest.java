package com.gymrats.api.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gymrats.api.user.domain.UserRepository;
import com.gymrats.api.workout.domain.WorkoutSession;
import com.gymrats.api.workout.domain.WorkoutSessionSource;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BetaImprovementsIntegrationTest {
  private static final UUID SQUAT = UUID.fromString("10000000-0000-0000-0000-000000000001");
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired JdbcTemplate jdbc;

  @Test
  void requiresAndPersistsExplicitRegistrationConsent() throws Exception {
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"name":"No consent","email":"no-consent@example.com","password":"password123"}
            """))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"name":"No consent","email":"no-consent@example.com","password":"password123","acceptedTerms":false}
            """))
        .andExpect(status().isBadRequest());
    var token = register("consent@example.com", true);
    var stored = users.findByEmailIgnoreCase("consent@example.com").orElseThrow();
    assertThat(stored.isPhotoStorageConsent()).isTrue();
    assertThat(stored.getTermsAcceptedAt()).isNotNull();
    assertThat(stored.getTermsVersion()).isEqualTo("2026-11-26");
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk()).andExpect(jsonPath("$.photoStorageConsent").value(true))
        .andExpect(jsonPath("$.termsVersion").value("2026-11-26"));
  }

  @Test
  void returnsOnlyOwnerCompletedHistoryAndAllTimeRecords() throws Exception {
    var owner = register("history-owner@example.com", false);
    var stranger = register("history-stranger@example.com", false);
    users.flush();
    UUID ownerId = users.findByEmailIgnoreCase("history-owner@example.com").orElseThrow().getId();
    UUID strangerId = users.findByEmailIgnoreCase("history-stranger@example.com").orElseThrow().getId();
    Instant now = Instant.now();
    seed(ownerId, now.minusSeconds(100), "COMPLETED", 40, 10);
    seed(ownerId, now, "COMPLETED", 50, 8);
    seed(ownerId, now.plusSeconds(100), "SKIPPED", 999, 99);
    seed(strangerId, now, "COMPLETED", 200, 3);
    mvc.perform(get("/api/v1/exercises/{id}/history", SQUAT).header("Authorization", "Bearer " + owner))
        .andExpect(status().isOk()).andExpect(jsonPath("$.exerciseName").value("Agachamento livre"))
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].sets[0].loadKg").value(50))
        .andExpect(jsonPath("$.summary.sessionCount").value(2))
        .andExpect(jsonPath("$.summary.totalSets").value(2))
        .andExpect(jsonPath("$.summary.totalVolumeKg").value(800))
        .andExpect(jsonPath("$.summary.maxLoadKg").value(50))
        .andExpect(jsonPath("$.summary.maxRepetitions").value(10));
    mvc.perform(get("/api/v1/exercises/{id}/history", SQUAT).header("Authorization", "Bearer " + stranger))
        .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.summary.totalVolumeKg").value(600));
    mvc.perform(get("/api/v1/exercises/{id}/history", SQUAT)).andExpect(status().isUnauthorized());
  }

  @Test
  void boundsRecentHistoryWithoutTruncatingAllTimeSummary() throws Exception {
    var token = register("history-bounded@example.com", false);
    users.flush();
    UUID owner = users.findByEmailIgnoreCase("history-bounded@example.com").orElseThrow().getId();
    for (int index = 0; index < 32; index++) seed(owner, Instant.now().plusSeconds(index), "COMPLETED", 10, 10);
    mvc.perform(get("/api/v1/exercises/{id}/history", SQUAT).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(30))
        .andExpect(jsonPath("$.summary.sessionCount").value(32))
        .andExpect(jsonPath("$.summary.totalVolumeKg").value(3200));
  }

  @Test
  void createsFeedbackReceiptAndStripsQueryData() throws Exception {
    var token = register("feedback@example.com", false);
    users.flush();
    var request = """
        {"category":"BUG","description":"Não consegui concluir meu treino.","path":"/workout-session?token=secret#fragment","requestId":"audit-123"}
        """;
    var result = mvc.perform(post("/api/v1/feedback").header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.createdAt").exists()).andReturn();
    String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    assertThat(jdbc.queryForObject("select path from beta_feedback where id = ?", String.class, UUID.fromString(id)))
        .isEqualTo("/workout-session");
    mvc.perform(post("/api/v1/feedback").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/feedback").header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON).content("""
            {"category":"BUG","description":"x          ","path":"https://example.com"}
            """))
        .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/v1/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    assertThat(jdbc.queryForObject("select count(*) from beta_feedback where id = ?", Long.class,
        UUID.fromString(id))).isZero();
  }

  @Test
  void correlatesAllResponsesAndSupportsBrowserIdempotencyHeaders() throws Exception {
    mvc.perform(get("/actuator/health").header("X-Request-Id", "audit-valid_123"))
        .andExpect(status().isOk()).andExpect(header().string("X-Request-Id", "audit-valid_123"));
    var generated = mvc.perform(get("/api/v1/plans").header("X-Request-Id", "unsafe id"))
        .andExpect(status().isUnauthorized()).andReturn().getResponse().getHeader("X-Request-Id");
    assertThat(generated).matches("[A-Za-z0-9._-]{1,80}").isNotEqualTo("unsafe id");
    mvc.perform(options("/api/v1/workout-sessions").header("Origin", "http://localhost:3000")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "content-type,idempotency-key,x-request-id"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Headers", containsString("idempotency-key")))
        .andExpect(header().string("Access-Control-Allow-Headers", containsString("x-request-id")));
  }

  @Test
  void timestampsRemainStableAtPostgresqlPrecision() {
    var user = new com.gymrats.api.user.domain.User("Timestamp", "timestamp@example.com", "hash");
    var session = new WorkoutSession(user, WorkoutSessionSource.FREE, null);
    session.complete();
    assertThat(session.getStartedAt().getNano() % 1000).isZero();
    assertThat(session.getCompletedAt().getNano() % 1000).isZero();
  }

  private String register(String email, boolean photoConsent) throws Exception {
    var response = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"name":"QA Beta","email":"%s","password":"password123","acceptedTerms":true,"photoStorageConsent":%s}
            """.formatted(email, photoConsent)))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return JsonPath.read(response, "$.accessToken");
  }

  private void seed(UUID owner, Instant completedAt, String exerciseStatus, int load, int reps) {
    UUID session = UUID.randomUUID(), exercise = UUID.randomUUID();
    var now = Timestamp.from(completedAt);
    jdbc.update("""
        INSERT INTO workout_sessions(id,owner_id,status,source,started_at,completed_at,created_at,updated_at)
        VALUES (?,?,'COMPLETED','FREE',?,?,?,?)
        """, session, owner, now, now, now, now);
    jdbc.update("""
        INSERT INTO workout_session_exercises(id,session_id,exercise_id,position,status)
        VALUES (?,?,?,0,?)
        """, exercise, session, SQUAT, exerciseStatus);
    jdbc.update("""
        INSERT INTO workout_session_sets(id,session_exercise_id,position,load_kg,repetitions)
        VALUES (?,?,0,?,?)
        """, UUID.randomUUID(), exercise, BigDecimal.valueOf(load), reps);
  }
}
