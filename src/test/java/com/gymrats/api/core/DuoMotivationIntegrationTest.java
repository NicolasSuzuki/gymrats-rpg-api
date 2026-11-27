package com.gymrats.api.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DuoMotivationIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired com.gymrats.api.user.domain.UserRepository users;

  private record Account(String token, UUID id) {}

  private Account account() throws Exception {
    String email = "duo-" + UUID.randomUUID() + "@example.com";
    var result =
        mvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Pessoa QA\",\"email\":\""
                            + email
                            + "\",\"password\":\"password123\",\"acceptedTerms\":true}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    users.flush();
    return new Account(
        JsonPath.read(result, "$.accessToken"),
        jdbc.queryForObject("SELECT id FROM users WHERE email=?", UUID.class, email));
  }

  private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, Account user) {
    return request.header("Authorization", "Bearer " + user.token());
  }

  private String json(MockHttpServletRequestBuilder request, Account user) throws Exception {
    return mvc.perform(auth(request, user))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String pair(Account a, Account b) throws Exception {
    String invite =
        mvc.perform(auth(post("/api/v1/duo/invites"), a))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(invite, "$.token");
    mvc.perform(auth(post("/api/v1/duo/invites/{token}/join", token), b))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    return token;
  }

  private String workout(Account a) throws Exception {
    return mvc.perform(
            auth(post("/api/v1/workout-sessions/quick-check-in"), a)
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private void goal(Account a, int target) throws Exception {
    mvc.perform(
            auth(patch("/api/v1/users/me"), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weeklyGoal\":" + target + "}"))
        .andExpect(status().isOk());
  }

  @Test
  void protectsInvitesAndRequiresOnlyTwoActiveMembers() throws Exception {
    var a = account();
    var b = account();
    var c = account();
    mvc.perform(get("/api/v1/duo")).andExpect(status().isUnauthorized());
    String token = pair(a, b);
    mvc.perform(auth(post("/api/v1/duo/invites/{token}/join", token), b))
        .andExpect(status().isOk());
    mvc.perform(auth(post("/api/v1/duo/invites/{token}/join", token), c))
        .andExpect(status().isConflict());
    mvc.perform(auth(post("/api/v1/duo/invites"), a)).andExpect(status().isConflict());
    mvc.perform(auth(get("/api/v1/duo"), c)).andExpect(jsonPath("$.status").value("NONE"));
    mvc.perform(auth(get("/api/v1/duo"), a)).andExpect(jsonPath("$.members.length()").value(2));
  }

  @Test
  void countsDistinctDaysAndBalancesIndependentGoals() throws Exception {
    var a = account();
    var b = account();
    goal(a, 1);
    goal(b, 2);
    pair(a, b);
    workout(a);
    workout(a);
    workout(b);
    var state = json(get("/api/v1/duo"), a);
    assertThat((Double) JsonPath.read(state, "$.cooperative.progress")).isEqualTo(0.75);
    assertThat((Boolean) JsonPath.read(state, "$.cooperative.completed")).isFalse();
    assertThat((Integer) JsonPath.read(json(get("/api/v1/motivation"), a), "$.nextStep.current"))
        .isEqualTo(1);
    goal(b, 1);
    mvc.perform(auth(get("/api/v1/duo"), a))
        .andExpect(jsonPath("$.cooperative.completed").value(true));
  }

  @Test
  void privatePhotoNeverAppearsInPublicFeedAndAccessEndsWithPartnership() throws Exception {
    var a = account();
    var b = account();
    var stranger = account();
    pair(a, b);
    String session = JsonPath.read(workout(a), "$.id");
    var image =
        new MockMultipartFile(
            "photo", "qa.png", "image/png", new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 1});
    mvc.perform(
            multipart("/api/v1/workout-sessions/{id}/photo", session)
                .file(image)
                .header("Authorization", "Bearer " + a.token()))
        .andExpect(status().isCreated());
    String payload =
        "{\"sessionId\":\"" + session + "\",\"caption\":\"Nossa lembrança\",\"includePhoto\":true}";
    String panel =
        json(
            post("/api/v1/duo/memories")
                .header("Idempotency-Key", "memory-one")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
            a);
    String memory = JsonPath.read(panel, "$.memories[0].id");
    String url = JsonPath.read(panel, "$.memories[0].photoUrl");
    String photo = url.substring(url.lastIndexOf('/') + 1);
    mvc.perform(
            auth(post("/api/v1/duo/memories"), a)
                .header("Idempotency-Key", "memory-one")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memories.length()").value(1));
    mvc.perform(auth(get("/api/v1/duo/media/{id}", photo), b))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
    mvc.perform(auth(get("/api/v1/duo/media/{id}", photo), stranger))
        .andExpect(status().isNotFound());
    mvc.perform(auth(get("/api/v1/media/{id}", photo), stranger)).andExpect(status().isNotFound());
    mvc.perform(auth(get("/api/v1/feed"), stranger))
        .andExpect(jsonPath("$.items.length()").value(0));
    mvc.perform(auth(delete("/api/v1/duo/memories/{id}", memory), b))
        .andExpect(status().isForbidden());
    mvc.perform(
            auth(put("/api/v1/duo/memories/{id}/reaction", memory), b)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"PROUD\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memories[0].reactions[0].count").value(1));
    mvc.perform(auth(delete("/api/v1/duo"), a)).andExpect(status().isNoContent());
    mvc.perform(auth(get("/api/v1/duo/media/{id}", photo), b)).andExpect(status().isNotFound());
    mvc.perform(auth(get("/api/v1/duo"), b)).andExpect(jsonPath("$.status").value("NONE"));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM duo_private_media", Long.class)).isZero();
  }

  @Test
  void meetingRequiresOtherPersonAndSettingsControlReminders() throws Exception {
    var a = account();
    var b = account();
    pair(a, b);
    String scheduled = Instant.now().plus(2, ChronoUnit.DAYS).toString();
    String state =
        json(
            post("/api/v1/duo/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduledAt\":\"" + scheduled + "\",\"location\":\"Academia\"}"),
            a);
    String id = JsonPath.read(state, "$.meetings[0].id");
    String expected = JsonPath.read(state, "$.meetings[0].scheduledAt");
    String decision = "{\"action\":\"ACCEPT\",\"expectedScheduledAt\":\"" + expected + "\"}";
    mvc.perform(
            auth(patch("/api/v1/duo/meetings/{id}", id), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content(decision))
        .andExpect(status().isForbidden());
    mvc.perform(
            auth(patch("/api/v1/duo/meetings/{id}", id), b)
                .contentType(MediaType.APPLICATION_JSON)
                .content(decision))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reminders.length()").value(1));
    mvc.perform(
            auth(patch("/api/v1/duo/settings"), b)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"remindersEnabled\":false,\"encouragementEnabled\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reminders.length()").value(0));
    mvc.perform(auth(get("/api/v1/duo"), a))
        .andExpect(jsonPath("$.settings.remindersEnabled").value(true));
  }

  @Test
  void validatesCosmeticOwnershipAndPersistsSelections() throws Exception {
    var a = account();
    mvc.perform(
            auth(patch("/api/v1/motivation/rewards/selection"), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titleId\":\"Implacável\"}"))
        .andExpect(status().isConflict());
    workout(a);
    mvc.perform(
            auth(patch("/api/v1/motivation/rewards/selection"), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titleId\":\"Desperto\",\"frameId\":\"vanguard\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.selected.frameId").value("vanguard"));
    mvc.perform(auth(get("/api/v1/motivation"), a))
        .andExpect(jsonPath("$.selected.titleId").value("Desperto"));
    assertThat(
            jdbc.queryForObject(
                "SELECT selected_title FROM users WHERE id=?", String.class, a.id()))
        .isEqualTo("Desperto");
  }

  @Test
  void welcomesReturningUserWithoutRemovingEarnedProgressAndCleansDeletedAccount()
      throws Exception {
    var a = account();
    var b = account();
    pair(a, b);
    String id = JsonPath.read(workout(a), "$.id");
    jdbc.update(
        "UPDATE workout_sessions SET completed_at=? WHERE id=?",
        Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS)),
        UUID.fromString(id));
    mvc.perform(auth(get("/api/v1/motivation"), a))
        .andExpect(jsonPath("$.returningAfterPause").value(true))
        .andExpect(jsonPath("$.nextStep.current").value(1));
    mvc.perform(auth(delete("/api/v1/users/me"), a)).andExpect(status().isNoContent());
    mvc.perform(auth(get("/api/v1/duo"), b)).andExpect(jsonPath("$.status").value("NONE"));
  }

  @Test
  void cancelPendingInviteCannotDissolveAPartnershipFormedMeanwhile() throws Exception {
    var a = account();
    var b = account();
    String invitation =
        mvc.perform(auth(post("/api/v1/duo/invites"), a))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(invitation, "$.token");
    mvc.perform(auth(post("/api/v1/duo/invites/{token}/join", token), b))
        .andExpect(status().isOk());
    mvc.perform(auth(delete("/api/v1/duo/invites"), a)).andExpect(status().isNoContent());
    mvc.perform(auth(get("/api/v1/duo"), a)).andExpect(jsonPath("$.status").value("ACTIVE"));
    mvc.perform(auth(get("/api/v1/duo"), b)).andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void revokesOtherPendingInvitesWhenJoiningAndDoesNotReactivateThemOnLeave() throws Exception {
    var a = account();
    var b = account();
    var c = account();
    String old =
        mvc.perform(auth(post("/api/v1/duo/invites"), b))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String oldToken = JsonPath.read(old, "$.token");
    pair(a, b);
    mvc.perform(auth(get("/api/v1/duo/invites/{token}", oldToken), c))
        .andExpect(status().isNotFound());
    mvc.perform(auth(delete("/api/v1/duo"), a)).andExpect(status().isNoContent());
    mvc.perform(auth(post("/api/v1/duo/invites/{token}/join", oldToken), c))
        .andExpect(status().isNotFound());
    mvc.perform(auth(get("/api/v1/duo"), b)).andExpect(jsonPath("$.status").value("NONE"));
    var fresh =
        mvc.perform(auth(post("/api/v1/duo/invites"), b))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String freshToken = JsonPath.read(fresh, "$.token");
    mvc.perform(auth(delete("/api/v1/duo/invites"), b)).andExpect(status().isNoContent());
    mvc.perform(auth(get("/api/v1/duo/invites/{token}", freshToken), c))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsAStaleDecisionAfterMeetingReschedule() throws Exception {
    var a = account();
    var b = account();
    pair(a, b);
    String initial =
        json(
            post("/api/v1/duo/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduledAt\":\"" + Instant.now().plus(2, ChronoUnit.DAYS) + "\"}"),
            a);
    String id = JsonPath.read(initial, "$.meetings[0].id"),
        oldTime = JsonPath.read(initial, "$.meetings[0].scheduledAt");
    String updated =
        json(
            patch("/api/v1/duo/meetings/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"action\":\"RESCHEDULE\",\"scheduledAt\":\""
                        + Instant.now().plus(3, ChronoUnit.DAYS)
                        + "\"}"),
            a);
    mvc.perform(
            auth(patch("/api/v1/duo/meetings/{id}", id), b)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"ACCEPT\",\"expectedScheduledAt\":\"" + oldTime + "\"}"))
        .andExpect(status().isConflict());
    mvc.perform(
            auth(patch("/api/v1/duo/meetings/{id}", id), b)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"DECLINE\"}"))
        .andExpect(status().isBadRequest());
    String nextTime = JsonPath.read(updated, "$.meetings[0].scheduledAt");
    mvc.perform(
            auth(patch("/api/v1/duo/meetings/{id}", id), b)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"ACCEPT\",\"expectedScheduledAt\":\"" + nextTime + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meetings[0].status").value("ACCEPTED"));
  }

  @Test
  void nearTermAcceptedReminderSurvivesLimitedMeetingHistory() throws Exception {
    var a = account();
    var b = account();
    pair(a, b);
    UUID duo = UUID.fromString(JsonPath.read(json(get("/api/v1/duo"), a), "$.id"));
    for (int index = 0; index < 31; index++) {
      Instant scheduled = Instant.now().plus(10 + index, ChronoUnit.DAYS);
      jdbc.update(
          "INSERT INTO duo_meetings(id,duo_id,proposer_id,scheduled_at,status,created_at)"
              + " VALUES(?,?,?,?,'ACCEPTED',?)",
          UUID.randomUUID(),
          duo,
          a.id(),
          Timestamp.from(scheduled),
          Timestamp.from(Instant.now()));
    }
    UUID near = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO duo_meetings(id,duo_id,proposer_id,scheduled_at,status,created_at)"
            + " VALUES(?,?,?,?,'ACCEPTED',?)",
        near,
        duo,
        a.id(),
        Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS)),
        Timestamp.from(Instant.now()));
    UUID pending = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO duo_meetings(id,duo_id,proposer_id,scheduled_at,status,created_at)"
            + " VALUES(?,?,?,?,'PENDING',?)",
        pending,
        duo,
        a.id(),
        Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)),
        Timestamp.from(Instant.now()));
    mvc.perform(auth(get("/api/v1/duo"), b))
        .andExpect(jsonPath("$.meetings.length()").value(30))
        .andExpect(jsonPath("$.meetings[0].id").value(pending.toString()))
        .andExpect(jsonPath("$.meetings[0].canRespond").value(true))
        .andExpect(jsonPath("$.meetings[1].id").value(near.toString()))
        .andExpect(jsonPath("$.reminders.length()").value(1))
        .andExpect(jsonPath("$.reminders[0].id").value(near.toString()));
  }

  @Test
  void legacyTitleEndpointUsesTheCanonicalSelectionAndDayUnlocks() throws Exception {
    var a = account();
    workout(a);
    mvc.perform(
            auth(patch("/api/v1/profile/title"), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Desperto\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Desperto"));
    mvc.perform(auth(get("/api/v1/motivation"), a))
        .andExpect(jsonPath("$.selected.titleId").value("Desperto"));
    mvc.perform(
            auth(patch("/api/v1/profile/title"), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Implacável\"}"))
        .andExpect(status().isConflict());
    mvc.perform(
            auth(patch("/api/v1/profile/title"), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}"))
        .andExpect(status().isOk());
    mvc.perform(auth(get("/api/v1/motivation"), a))
        .andExpect(jsonPath("$.selected.titleId").value(""));
    assertThat(
            jdbc.queryForObject(
                "SELECT selected_title FROM users WHERE id=?", String.class, a.id()))
        .isNull();
  }

  @Test
  void invalidPrivateResourceIdentifiersAndReactionParametersReturnBadRequest() throws Exception {
    var a = account();
    mvc.perform(auth(get("/api/v1/duo/media/not-a-uuid"), a))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.fields[0].field").value("id"));
    mvc.perform(auth(delete("/api/v1/duo/memories/not-a-uuid/reaction").param("type", "PROUD"), a))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    String path = "/api/v1/duo/memories/" + UUID.randomUUID() + "/reaction";
    mvc.perform(auth(delete(path).param("type", "UNSUPPORTED"), a))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields[0].field").value("type"));
    mvc.perform(auth(delete(path), a))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.fields[0].field").value("type"));
  }

  @Test
  void preservesEarnedLegacyTitlesFromAccountsWithoutMotivationPreferences() throws Exception {
    String[][] legacyTitles = {{"Vanguarda", "WEEKLY_THREE"}, {"Ascendente", "XP_500"}};
    for (String[] legacy : legacyTitles) {
      var a = account();
      seedLegacyTitle(a, legacy[0], legacy[1]);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM motivation_preferences WHERE user_id=?",
                  Long.class,
                  a.id()))
          .isZero();
      String result = json(get("/api/v1/motivation"), a);
      assertThat((String) JsonPath.read(result, "$.selected.titleId")).isEqualTo(legacy[0]);
      java.util.List<java.util.Map<String, Object>> rewards = JsonPath.read(result, "$.rewards");
      var reward =
          rewards.stream().filter(r -> legacy[0].equals(r.get("id"))).findFirst().orElseThrow();
      assertThat(reward.get("progressUnit")).isEqualTo("conquista");
      assertThat(reward.get("current")).isEqualTo(1);
      assertThat(reward.get("target")).isEqualTo(1);
      assertThat(reward.get("unlocked")).isEqualTo(true);
      assertThat(reward.get("selected")).isEqualTo(true);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM motivation_preferences WHERE user_id=?",
                  Long.class,
                  a.id()))
          .isZero();
      mvc.perform(
              auth(patch("/api/v1/profile/title"), a)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"title\":\"\"}"))
          .andExpect(status().isOk());
      mvc.perform(
              auth(patch("/api/v1/profile/title"), a)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"title\":\"" + legacy[0] + "\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value(legacy[0]));
      mvc.perform(auth(get("/api/v1/motivation"), a))
          .andExpect(jsonPath("$.selected.titleId").value(legacy[0]));
      assertThat(
              jdbc.queryForObject(
                  "SELECT selected_title FROM users WHERE id=?", String.class, a.id()))
          .isEqualTo(legacy[0]);
    }
  }

  @Test
  void legacyRewardsRequireTheirOriginalAchievementsAndDoNotGrantTheNewDayRewards()
      throws Exception {
    var a = account();
    for (String title : java.util.List.of("Vanguarda", "Ascendente")) {
      mvc.perform(
              auth(patch("/api/v1/motivation/rewards/selection"), a)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"titleId\":\"" + title + "\"}"))
          .andExpect(status().isConflict());
    }
    seedLegacyTitle(a, "Vanguarda", "WEEKLY_THREE");
    mvc.perform(
            auth(patch("/api/v1/motivation/rewards/selection"), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titleId\":\"Disciplinado\"}"))
        .andExpect(status().isConflict());
    mvc.perform(
            auth(patch("/api/v1/motivation/rewards/selection"), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titleId\":\"Ascendente\"}"))
        .andExpect(status().isConflict());
    // A selected string alone is not evidence that a title was ever earned.
    var user = users.findById(a.id()).orElseThrow();
    user.selectTitle("Ascendente");
    users.saveAndFlush(user);
    mvc.perform(auth(get("/api/v1/motivation"), a))
        .andExpect(jsonPath("$.selected.titleId").value(""));
  }

  @Test
  void preservesTheOriginalFirstWorkoutTitleWithoutInventingActivityDays() throws Exception {
    var a = account();
    seedLegacyTitle(a, "Desperto", "FIRST_WORKOUT");
    String result = json(get("/api/v1/motivation"), a);
    assertThat((String) JsonPath.read(result, "$.selected.titleId")).isEqualTo("Desperto");
    assertThat((Integer) JsonPath.read(result, "$.nextStep.current")).isZero();
    java.util.List<java.util.Map<String, Object>> rewards = JsonPath.read(result, "$.rewards");
    assertThat(
            rewards.stream()
                .filter(r -> "Desperto".equals(r.get("id")))
                .findFirst()
                .orElseThrow()
                .get("unlocked"))
        .isEqualTo(true);
    mvc.perform(
            auth(patch("/api/v1/motivation/rewards/selection"), a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titleId\":\"Desperto\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void ordersUpcomingMeetingsSoonestFirstAndPastMeetingsMostRecentFirst() throws Exception {
    var a = account();
    var b = account();
    pair(a, b);
    UUID duo = UUID.fromString(JsonPath.read(json(get("/api/v1/duo"), a), "$.id"));
    UUID[] ids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
    int[] offsets = {2, -3, 1, -1};
    for (int index = 0; index < ids.length; index++) {
      jdbc.update(
          "INSERT INTO duo_meetings(id,duo_id,proposer_id,scheduled_at,status,created_at)"
              + " VALUES(?,?,?,?,'ACCEPTED',?)",
          ids[index],
          duo,
          a.id(),
          Timestamp.from(Instant.now().plus(offsets[index], ChronoUnit.DAYS)),
          Timestamp.from(Instant.now()));
    }
    mvc.perform(auth(get("/api/v1/duo"), b))
        .andExpect(jsonPath("$.meetings[0].id").value(ids[2].toString()))
        .andExpect(jsonPath("$.meetings[1].id").value(ids[0].toString()))
        .andExpect(jsonPath("$.meetings[2].id").value(ids[3].toString()))
        .andExpect(jsonPath("$.meetings[3].id").value(ids[1].toString()));
  }

  private void seedLegacyTitle(Account a, String title, String code) {
    var user = users.findById(a.id()).orElseThrow();
    user.selectTitle(title);
    users.saveAndFlush(user);
    var now = Timestamp.from(Instant.now());
    jdbc.update(
        "INSERT INTO user_achievements(id,user_id,code,unlocked_at,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?)",
        UUID.randomUUID(),
        a.id(),
        code,
        now,
        now,
        now);
  }
}
