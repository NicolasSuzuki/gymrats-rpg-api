package com.gymrats.api.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

import com.gymrats.api.user.domain.UserRepository;
import com.gymrats.api.user.domain.WeeklyGoalVersionRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CoreBackendFlowIntegrationTest {
  private static final UUID SQUAT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID BENCH_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired WeeklyGoalVersionRepository goals;

  @Test
  void persistsProfileOnboardingAndWeeklyGoalHistory() throws Exception {
    var token = register("profile@example.com");

    mvc.perform(authorized(patch("/api/v1/users/me/onboarding"), token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"modalities":["ACADEMIA","CORRIDA"],"weeklyGoal":3,"characterClass":"RANGER"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.onboardingCompleted").value(true))
        .andExpect(jsonPath("$.weeklyGoal").value(3))
        .andExpect(jsonPath("$.characterClass").value("RANGER"));

    mvc.perform(authorized(patch("/api/v1/users/me"), token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Perfil Atualizado","weeklyGoal":5,"universe":"fitness"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Perfil Atualizado"))
        .andExpect(jsonPath("$.weeklyGoal").value(5))
        .andExpect(jsonPath("$.universe").value("fitness"));

    var user = users.findByEmailIgnoreCase("profile@example.com").orElseThrow();
    org.assertj.core.api.Assertions.assertThat(
            goals.findByUserIdOrderByValidFromDesc(user.getId()))
        .hasSize(2)
        .satisfies(items -> {
          org.assertj.core.api.Assertions.assertThat(items.get(0).getTarget()).isEqualTo(5);
          org.assertj.core.api.Assertions.assertThat(items.get(1).getValidUntil()).isNotNull();
        });
  }

  @Test
  void filtersCatalogAndProtectsPersonalExercisesByOwner() throws Exception {
    var ownerToken = register("exercise-owner@example.com");
    var strangerToken = register("exercise-stranger@example.com");

    mvc.perform(authorized(
            get("/api/v1/exercises").param("modality", "ACADEMIA").param("name", "supino"),
            ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].id").value(hasItem(BENCH_ID.toString())));

    var created = mvc.perform(authorized(post("/api/v1/exercises/personal"), ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Meu exercício","modality":"ACADEMIA","category":"Força"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.visibility").value("PERSONAL"))
        .andReturn();
    String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mvc.perform(authorized(get("/api/v1/exercises/{id}", id), ownerToken))
        .andExpect(status().isOk());
    mvc.perform(authorized(get("/api/v1/exercises/{id}", id), strangerToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void versionsDuplicatesArchivesAndRestoresPlans() throws Exception {
    var token = register("plans@example.com");
    var first = createPlan(token, "Plano A");
    String firstId = JsonPath.read(first, "$.id");

    var versioned = mvc.perform(authorized(put("/api/v1/plans/{id}", firstId), token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(planBody("Plano B")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.previousVersionId").value(firstId))
        .andReturn();
    String currentId = JsonPath.read(versioned.getResponse().getContentAsString(), "$.id");

    mvc.perform(authorized(get("/api/v1/plans/{id}", firstId), token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentVersion").value(false));
    mvc.perform(authorized(get("/api/v1/plans"), token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.items[0].id").value(currentId));

    var duplicate = mvc.perform(authorized(post("/api/v1/plans/{id}/duplicate", currentId), token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(1))
        .andReturn();
    String duplicateId = JsonPath.read(duplicate.getResponse().getContentAsString(), "$.id");
    mvc.perform(authorized(post("/api/v1/plans/{id}/archive", duplicateId), token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));
    mvc.perform(authorized(post("/api/v1/plans/{id}/restore", duplicateId), token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void enforcesSingleActiveSessionAutosaveAndTerminalIdempotency() throws Exception {
    var token = register("sessions@example.com");
    String planId = JsonPath.read(createPlan(token, "Plano sessão"), "$.id");

    var started = mvc.perform(authorized(post("/api/v1/workout-sessions"), token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":\"" + planId + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.source").value("PLAN"))
        .andExpect(jsonPath("$.exercises[0].exerciseName").value("Agachamento livre"))
        .andExpect(jsonPath("$.planSnapshot.version").value(1))
        .andReturn();
    String sessionId = JsonPath.read(started.getResponse().getContentAsString(), "$.id");

    mvc.perform(authorized(post("/api/v1/workout-sessions"), token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"FREE\"}"))
        .andExpect(status().isConflict());

    mvc.perform(authorized(
            patch("/api/v1/workout-sessions/{id}/exercises/{exerciseId}", sessionId, SQUAT_ID),
            token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"COMPLETED","data":{"notes":"Boa execução","sets":[{"loadKg":40,"repetitions":10,"rpe":7}]}}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.sets[0].repetitions").value(10));

    mvc.perform(authorized(post("/api/v1/workout-sessions/{id}/complete", sessionId), token)
            .header("Idempotency-Key", "complete:" + sessionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.summary.completedExercises").value(1));
    mvc.perform(authorized(post("/api/v1/workout-sessions/{id}/complete", sessionId), token)
            .header("Idempotency-Key", "complete:" + sessionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    var free = mvc.perform(authorized(post("/api/v1/workout-sessions"), token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"FREE\",\"exerciseIds\":[\"" + BENCH_ID + "\"]}"))
        .andExpect(status().isCreated())
        .andReturn();
    String freeId = JsonPath.read(free.getResponse().getContentAsString(), "$.id");
    mvc.perform(authorized(post("/api/v1/workout-sessions/{id}/cancel", freeId), token)
            .header("Idempotency-Key", "cancel:" + freeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void completesQuickCheckInWithoutExerciseDetailsAndReplaysIdempotently()
      throws Exception {
    var token = register("quick-check-in@example.com");
    var first = mvc.perform(authorized(post("/api/v1/workout-sessions/quick-check-in"), token)
            .header("Idempotency-Key", "quick-check-in:2026-08-09"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.source").value("FREE"))
        .andExpect(jsonPath("$.summary.totalExercises").value(0))
        .andExpect(jsonPath("$.xpAwarded").value(50))
        .andReturn();
    String sessionId = JsonPath.read(first.getResponse().getContentAsString(), "$.id");

    mvc.perform(authorized(post("/api/v1/workout-sessions/quick-check-in"), token)
            .header("Idempotency-Key", "quick-check-in:2026-08-09"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(sessionId));

    mvc.perform(authorized(get("/api/v1/progression"), token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalXp").value(50))
        .andExpect(jsonPath("$.weeklyCompleted").value(1));
  }

  @Test
  void validatesMediaPublishesOnceAndProtectsComments() throws Exception {
    var ownerToken = register("feed-owner@example.com");
    var strangerToken = register("feed-stranger@example.com");
    String planId = JsonPath.read(createPlan(ownerToken, "Plano publicado"), "$.id");
    var started = mvc.perform(authorized(post("/api/v1/workout-sessions"), ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"planId\":\"" + planId + "\"}"))
        .andExpect(status().isCreated())
        .andReturn();
    String sessionId = JsonPath.read(started.getResponse().getContentAsString(), "$.id");
    mvc.perform(authorized(post("/api/v1/workout-sessions/{id}/complete", sessionId), ownerToken)
            .header("Idempotency-Key", "complete-feed:" + sessionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.xpAwarded").value(50));

    var invalid = new MockMultipartFile(
        "photo", "not-image.txt", "text/plain", "invalid".getBytes());
    mvc.perform(multipart("/api/v1/workout-sessions/{id}/photo", sessionId)
            .file(invalid)
            .header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isConflict());

    byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    var photo = new MockMultipartFile("photo", "treino.png", "image/png", png);
    var uploaded = mvc.perform(multipart("/api/v1/workout-sessions/{id}/photo", sessionId)
            .file(photo)
            .header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mimeType").value("image/png"))
        .andReturn();
    String mediaId = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.id");

    var published = mvc.perform(authorized(post("/api/v1/workout-sessions/{id}/publish", sessionId), ownerToken)
            .header("Idempotency-Key", "publish:" + sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"caption\":\"Treino concluído\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.visual").value("PHOTO"))
        .andExpect(jsonPath("$.xpAwarded").value(50))
        .andReturn();
    String postId = JsonPath.read(published.getResponse().getContentAsString(), "$.id");

    mvc.perform(authorized(post("/api/v1/workout-sessions/{id}/publish", sessionId), ownerToken)
            .header("Idempotency-Key", "publish:" + sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(postId));

    mvc.perform(authorized(get("/api/v1/feed"), strangerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(postId))
        .andExpect(jsonPath("$.availableCategories[0]").value("WORKOUT"));
    mvc.perform(authorized(get("/api/v1/profile/workout-diary"), ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(postId));
    mvc.perform(authorized(get("/api/v1/media/{id}", mediaId), strangerToken))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
            .contentType("image/png"));

    var comment = mvc.perform(authorized(post("/api/v1/feed/{id}/comments", postId), ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"Bom treino!\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.canDelete").value(true))
        .andReturn();
    String commentId = JsonPath.read(comment.getResponse().getContentAsString(), "$.id");
    mvc.perform(authorized(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                "/api/v1/feed/{postId}/comments/{commentId}", postId, commentId),
            strangerToken))
        .andExpect(status().isForbidden());
    mvc.perform(authorized(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                "/api/v1/feed/{postId}/comments/{commentId}", postId, commentId),
            ownerToken))
        .andExpect(status().isNoContent());
    mvc.perform(authorized(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                "/api/v1/workout-sessions/{id}/photo", sessionId),
            ownerToken))
        .andExpect(status().isNoContent());
  }

  private String register(String email) throws Exception {
    var result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"QA Backend\",\"email\":\"" + email + "\",\"password\":\"password123\",\"acceptedTerms\":true}"))
        .andExpect(status().isCreated())
        .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
  }

  private String createPlan(String token, String name) throws Exception {
    return mvc.perform(authorized(post("/api/v1/plans"), token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(planBody(name)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String planBody(String name) {
    return """
        {"name":"%s","modality":"ACADEMIA","exerciseIds":["%s","%s"],"plannedParameters":[{"exerciseId":"%s","sets":3,"repetitions":10}]}
        """.formatted(name, SQUAT_ID, BENCH_ID, SQUAT_ID);
  }

  private MockHttpServletRequestBuilder authorized(
      MockHttpServletRequestBuilder request, String token) {
    return request.header("Authorization", "Bearer " + token);
  }
}
