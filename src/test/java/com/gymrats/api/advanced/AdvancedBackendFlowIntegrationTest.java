package com.gymrats.api.advanced;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.gymrats.api.progression.domain.AdvancedRank;
import com.gymrats.api.progression.domain.AdvancedRankProfile;
import com.gymrats.api.progression.domain.AdvancedRankProfileRepository;
import com.gymrats.api.progression.domain.MonarchReignRepository;
import com.gymrats.api.progression.domain.XpEvent;
import com.gymrats.api.progression.domain.XpEventRepository;
import com.gymrats.api.user.domain.UserRepository;
import com.gymrats.api.workout.domain.WorkoutSession;
import com.gymrats.api.workout.domain.WorkoutSessionRepository;
import com.gymrats.api.workout.domain.WorkoutSessionSource;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "app.progression.advanced.s5-weeks=0",
      "app.progression.advanced.s4-weeks=0",
      "app.progression.advanced.s3-weeks=0",
      "app.progression.advanced.s2-weeks=0",
      "app.progression.advanced.s1-weeks=0",
      "app.progression.advanced.protection-days=0"
    })
@AutoConfigureMockMvc
@Transactional
class AdvancedBackendFlowIntegrationTest {
  private static final UUID SQUAT_ID =
      UUID.fromString("10000000-0000-0000-0000-000000000001");

  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired XpEventRepository xpEvents;
  @Autowired AdvancedRankProfileRepository rankProfiles;
  @Autowired MonarchReignRepository monarchReigns;
  @Autowired WorkoutSessionRepository workoutSessions;
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManager entityManager;

  @Test
  void awardsProgressionOnceAndBuildsFairGlobalRanking() throws Exception {
    var alice = register("Alice", "advanced-alice@example.com");
    var bob = register("Bob", "advanced-bob@example.com");

    completeWorkout(alice, "alice-workout");
    completeWorkout(bob, "bob-workout");

    mvc.perform(authorized(get("/api/v1/progression"), alice.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalXp").value(50))
        .andExpect(jsonPath("$.level").value(1))
        .andExpect(jsonPath("$.weeklyCompleted").value(1));
    mvc.perform(authorized(get("/api/v1/progression/history"), alice.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].type").exists());
    mvc.perform(authorized(get("/api/v1/progression/achievements"), alice.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == 'FIRST_WORKOUT')].unlockedAt").exists());
    mvc.perform(authorized(patch("/api/v1/profile/title"), alice.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Desperto\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Desperto"));

    mvc.perform(authorized(
            get("/api/v1/rankings")
                .param("period", "WEEKLY")
                .param("metric", "CONSISTENCY"),
            alice.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scope").value("GLOBAL"))
        .andExpect(jsonPath("$.calculation").isNotEmpty())
        .andExpect(jsonPath("$.entries[0].value").value(1));
  }

  @Test
  void managesFriendshipsGroupsInvitesAndDeduplicatedNotifications() throws Exception {
    var alice = register("Alice", "social-alice@example.com");
    var bob = register("Bob", "social-bob@example.com");

    var sent = mvc.perform(authorized(post("/api/v1/social/friend-requests"), alice.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\"" + bob.id() + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn();
    String requestId = JsonPath.read(sent.getResponse().getContentAsString(), "$.id");

    mvc.perform(authorized(get("/api/v1/notifications"), bob.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(1))
        .andExpect(jsonPath("$.items[0].kind").value("FRIENDSHIP"));
    mvc.perform(authorized(patch("/api/v1/social/friend-requests/{id}", requestId), bob.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.id").value(alice.id().toString()))
        .andExpect(jsonPath("$.direction").value("RECEIVED"))
        .andExpect(jsonPath("$.status").value("ACCEPTED"));
    mvc.perform(authorized(get("/api/v1/social/friends"), alice.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(bob.id().toString()));

    var created = mvc.perform(authorized(post("/api/v1/social/groups"), alice.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Grupo QA","description":"Fluxo social","visibility":"PUBLIC"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andReturn();
    String groupId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mvc.perform(authorized(post("/api/v1/social/groups/{id}/join", groupId), bob.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memberCount").value(2));
    mvc.perform(authorized(delete("/api/v1/social/groups/{id}/membership", groupId), alice.token()))
        .andExpect(status().isConflict());
    mvc.perform(authorized(patch("/api/v1/social/groups/{id}/admin", groupId), alice.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\"" + bob.id() + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adminId").value(bob.id().toString()));
    mvc.perform(authorized(delete("/api/v1/social/groups/{id}/membership", groupId), alice.token()))
        .andExpect(status().isNoContent());

    var privateGroup = mvc.perform(authorized(post("/api/v1/social/groups"), bob.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Grupo privado","description":"Convite único","visibility":"PRIVATE"}
                """))
        .andExpect(status().isCreated())
        .andReturn();
    String privateId = JsonPath.read(privateGroup.getResponse().getContentAsString(), "$.id");
    var invite = mvc.perform(authorized(
            post("/api/v1/social/groups/{id}/invites", privateId), bob.token()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.available").value(true))
        .andReturn();
    String token = JsonPath.read(invite.getResponse().getContentAsString(), "$.token");
    mvc.perform(authorized(post("/api/v1/social/group-invites/{token}/join", token), alice.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("MEMBER"));
    mvc.perform(authorized(post("/api/v1/social/group-invites/{token}/join", token), alice.token()))
        .andExpect(status().isConflict());

    mvc.perform(authorized(patch("/api/v1/notifications"), bob.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(0));
  }

  @Test
  void deletesPersonalSurfaceAndInvalidatesExistingSession() throws Exception {
    var user = register("Delete Me", "delete-me@example.com");
    mvc.perform(authorized(delete("/api/v1/users/me"), user.token()))
        .andExpect(status().isNoContent());
    mvc.perform(authorized(get("/api/v1/users/me"), user.token()))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"delete-me@example.com","password":"password123"}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void preservesIndependentMasteryWhenTheActiveClassChanges() throws Exception {
    var user = register("Class Mastery", "class-mastery@example.com");
    mvc.perform(authorized(patch("/api/v1/users/me/onboarding"), user.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"modalities":["ACADEMIA"],"weeklyGoal":3,"characterClass":"FIGHTER"}
                """))
        .andExpect(status().isOk());

    completeWorkout(user, "fighter-mastery");
    mvc.perform(authorized(patch("/api/v1/users/me"), user.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"characterClass\":\"MAGE\"}"))
        .andExpect(status().isOk());
    completeWorkout(user, "mage-mastery");

    mvc.perform(authorized(get("/api/v1/progression/advanced"), user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activeClass").value("MAGE"))
        .andExpect(
            jsonPath("$.classMasteries[?(@.characterClass == 'FIGHTER')].totalXp")
                .value(50))
        .andExpect(
            jsonPath("$.classMasteries[?(@.characterClass == 'MAGE')].totalXp")
                .value(50))
        .andExpect(jsonPath("$.masteryHistory.length()").value(2));
  }

  @Test
  void promotesS1ToNationalAfterThirtyActiveEvaluationDays() throws Exception {
    var testUser = register("National", "national@example.com");
    var user = users.findById(testUser.id()).orElseThrow();
    Instant now = Instant.now();
    seedRankAXp(user, now.minus(Duration.ofDays(31)));
    var profile = new AdvancedRankProfile(user);
    profile.transition(
        AdvancedRank.S1,
        now.minus(Duration.ofDays(31)),
        now.minus(Duration.ofDays(30)));
    rankProfiles.save(profile);

    completeWorkout(testUser, "national-evaluation");

    mvc.perform(authorized(get("/api/v1/progression/advanced"), testUser.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentRank").value("Nacional"))
        .andExpect(jsonPath("$.nationalEvaluationDays").value(30))
        .andExpect(jsonPath("$.rankHistory[0].newRank").value("Nacional"));
  }

  @Test
  void legitimatePauseFreezesTheNationalEvaluationClock() throws Exception {
    var testUser = register("Paused", "paused-national@example.com");
    var user = users.findById(testUser.id()).orElseThrow();
    Instant now = Instant.now();
    seedRankAXp(user, now.minus(Duration.ofDays(31)));
    var profile = new AdvancedRankProfile(user);
    profile.transition(
        AdvancedRank.S1,
        now.minus(Duration.ofDays(31)),
        now.minus(Duration.ofDays(30)));
    profile.pause("Recuperação informada pelo usuário.", now.minus(Duration.ofDays(2)));
    rankProfiles.save(profile);

    completeWorkout(testUser, "paused-national-evaluation");

    mvc.perform(authorized(get("/api/v1/progression/advanced"), testUser.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentRank").value("S-1"))
        .andExpect(jsonPath("$.evaluationPaused").value(true))
        .andExpect(jsonPath("$.nationalEvaluationDays").value(29));
  }

  @Test
  void transfersTheSingleMonarchTitleAndPreservesTheHallOfFame() throws Exception {
    var alice = register("Monarch Alice", "monarch-alice@example.com");
    var bob = register("Monarch Bob", "monarch-bob@example.com");
    Instant now = Instant.now();
    promoteToNational(alice.id(), now.minus(Duration.ofDays(10)));
    promoteToNational(bob.id(), now.minus(Duration.ofDays(5)));

    var aliceUser = users.findById(alice.id()).orElseThrow();
    var bobUser = users.findById(bob.id()).orElseThrow();
    saveCompletedAt(aliceUser, now.minus(Duration.ofDays(3)));
    saveCompletedAt(aliceUser, now.minus(Duration.ofDays(3)).plusSeconds(3600));
    saveCompletedAt(bobUser, now.minus(Duration.ofDays(3)));
    entityManager.clear();

    mvc.perform(authorized(get("/api/v1/progression/hall-of-fame"), alice.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentMonarch.userId").value(alice.id().toString()))
        .andExpect(jsonPath("$.currentMonarch.currentUser").value(true))
        .andExpect(jsonPath("$.entries.length()").value(1));
    org.junit.jupiter.api.Assertions.assertEquals(
        1, monarchReigns.countByEndedAtIsNull());

    bobUser = users.findById(bob.id()).orElseThrow();
    saveCompletedAt(bobUser, now.minus(Duration.ofDays(2)));
    entityManager.clear();

    mvc.perform(authorized(get("/api/v1/progression/hall-of-fame"), alice.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentMonarch.userId").value(bob.id().toString()))
        .andExpect(jsonPath("$.currentMonarch.currentUser").value(false))
        .andExpect(jsonPath("$.entries.length()").value(2))
        .andExpect(
            jsonPath("$.entries[?(@.userId == '" + alice.id() + "')].reignCount")
                .value(1))
        .andExpect(
            jsonPath("$.entries[?(@.userId == '" + alice.id() + "')].currentMonarch")
                .value(false))
        .andExpect(
            jsonPath("$.entries[?(@.userId == '" + bob.id() + "')].currentMonarch")
                .value(true))
        .andExpect(
            jsonPath("$.entries[?(@.userId == '" + alice.id() + "')].reigns[0].endedAt")
                .exists());
    org.junit.jupiter.api.Assertions.assertEquals(
        1, monarchReigns.countByEndedAtIsNull());
    org.junit.jupiter.api.Assertions.assertEquals(2, monarchReigns.count());
  }

  private void completeWorkout(TestUser user, String key) throws Exception {
    var started = mvc.perform(authorized(post("/api/v1/workout-sessions"), user.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"FREE\",\"exerciseIds\":[\"" + SQUAT_ID + "\"]}"))
        .andExpect(status().isCreated())
        .andReturn();
    String id = JsonPath.read(started.getResponse().getContentAsString(), "$.id");
    mvc.perform(authorized(post("/api/v1/workout-sessions/{id}/complete", id), user.token())
            .header("Idempotency-Key", key))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.xpAwarded").value(50));
    mvc.perform(authorized(post("/api/v1/workout-sessions/{id}/complete", id), user.token())
            .header("Idempotency-Key", key))
        .andExpect(status().isOk());
  }

  private void seedRankAXp(
      com.gymrats.api.user.domain.User user, Instant occurredAt) {
    for (int index = 0; index < 139; index++) {
      xpEvents.save(
          new XpEvent(
              user,
              "WORKOUT_CHECK_IN",
              UUID.randomUUID(),
              50,
              occurredAt.plusSeconds(index)));
    }
  }

  private void promoteToNational(UUID userId, Instant nationalSince) {
    var profile = new AdvancedRankProfile(users.findById(userId).orElseThrow());
    profile.transition(AdvancedRank.NATIONAL, nationalSince, nationalSince);
    rankProfiles.saveAndFlush(profile);
  }

  private void saveCompletedAt(
      com.gymrats.api.user.domain.User user, Instant completedAt) {
    var session = new WorkoutSession(user, WorkoutSessionSource.FREE, null);
    session.complete();
    workoutSessions.saveAndFlush(session);
    jdbc.update(
        "update workout_sessions set started_at = ?, completed_at = ? where id = ?",
        Timestamp.from(completedAt.minus(Duration.ofHours(1))),
        Timestamp.from(completedAt),
        session.getId());
  }

  private TestUser register(String name, String email) throws Exception {
    var result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"name\":\""
                    + name
                    + "\",\"email\":\""
                    + email
                    + "\",\"password\":\"password123\"}"))
        .andExpect(status().isCreated())
        .andReturn();
    String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    var me = mvc.perform(authorized(get("/api/v1/users/me"), token))
        .andExpect(status().isOk())
        .andReturn();
    String id = JsonPath.read(me.getResponse().getContentAsString(), "$.id");
    return new TestUser(UUID.fromString(id), token);
  }

  private MockHttpServletRequestBuilder authorized(
      MockHttpServletRequestBuilder request, String token) {
    return request.header("Authorization", "Bearer " + token);
  }

  private record TestUser(UUID id, String token) {}
}
