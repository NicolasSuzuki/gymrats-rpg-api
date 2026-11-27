package com.gymrats.api.motivation.application;

import com.gymrats.api.duo.application.DuoService;
import com.gymrats.api.motivation.api.MotivationController.Selection;
import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.UserRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MotivationService {
  private final JdbcTemplate jdbc;
  private final UserRepository users;

  public MotivationService(JdbcTemplate jdbc, UserRepository users) {
    this.jdbc = jdbc;
    this.users = users;
  }

  public record Step(
      String id,
      String title,
      String description,
      long current,
      long target,
      String unit,
      String actionPath) {}

  public record Reward(
      String id,
      String type,
      String name,
      String description,
      String requirement,
      long current,
      int target,
      String progressUnit,
      boolean unlocked,
      boolean selected,
      String visualKey) {}

  public record Selected(String titleId, String frameId, String itemId) {}

  public record Response(
      Step nextStep,
      boolean returningAfterPause,
      Long daysSinceLastWorkout,
      List<Reward> rewards,
      Selected selected) {}

  private record Spec(String id, String type, String name, int days) {}

  private record LegacySpec(String id, String achievement, String requirement) {}

  private static final List<Spec> SPECS =
      List.of(
          new Spec("Desperto", "TITLE", "Desperto", 1),
          new Spec("Disciplinado", "TITLE", "Disciplinado", 7),
          new Spec("Implacável", "TITLE", "Implacável", 30),
          new Spec("vanguard", "FRAME", "Vanguarda", 1),
          new Spec("ascendant", "FRAME", "Ascendente", 30),
          new Spec("constancy-pin", "ITEM", "Broche da constância", 3),
          new Spec("journey-compass", "ITEM", "Bússola da jornada", 14),
          new Spec("sunrise-charm", "ITEM", "Amuleto do amanhecer", 60));

  private static final List<LegacySpec> LEGACY_TITLES =
      List.of(
          new LegacySpec(
              "Vanguarda", "WEEKLY_THREE", "3 dias distintos de atividade na mesma semana"),
          new LegacySpec("Ascendente", "XP_500", "500 XP validados"));

  @Transactional(readOnly = true)
  public Response response(User user) {
    var stats =
        jdbc.queryForObject(
            """
SELECT COUNT(DISTINCT CAST(completed_at AT TIME ZONE 'America/Sao_Paulo' AS DATE)) AS days,
  MAX(completed_at) AS latest FROM workout_sessions WHERE owner_id=? AND status='COMPLETED'
""",
            (rs, n) -> new Stats(rs.getLong("days"), rs.getObject("latest", OffsetDateTime.class)),
            user.getId());
    long days = stats.days();
    // Preserve the original, audited unlocks; do not reinterpret them as 7/30-day rewards.
    var earnedAchievements =
        new HashSet<>(
            jdbc.queryForList(
                "SELECT code FROM user_achievements WHERE user_id=? AND code IN"
                    + " ('FIRST_WORKOUT','WEEKLY_THREE','XP_500')",
                String.class,
                user.getId()));
    Long since =
        stats.latest() == null
            ? null
            : Math.max(
                0,
                ChronoUnit.DAYS.between(
                    stats.latest().atZoneSameInstant(DuoService.ZONE).toLocalDate(),
                    LocalDate.now(DuoService.ZONE)));
    var selected =
        jdbc
            .query(
                "SELECT title_id,frame_id,item_id FROM motivation_preferences WHERE user_id=?",
                (rs, n) -> new Selected(rs.getString(1), rs.getString(2), rs.getString(3)),
                user.getId())
            .stream()
            .findFirst()
            .orElseGet(
                () ->
                    new Selected(
                        availableTitle(user.getSelectedTitle(), days, earnedAchievements)
                            ? user.getSelectedTitle()
                            : "",
                        "none",
                        "none"));
    var rewards =
        new ArrayList<>(
            SPECS.stream()
                .map(
                    s ->
                        new Reward(
                            s.id(),
                            s.type(),
                            s.name(),
                            "Uma marca dos dias em que você escolheu se movimentar.",
                            s.days() == 1
                                ? "1 dia distinto de atividade"
                                : s.days() + " dias distintos de atividade",
                            s.id().equals("Desperto")
                                    && earnedAchievements.contains("FIRST_WORKOUT")
                                ? Math.max(1, days)
                                : days,
                            s.days(),
                            "dias",
                            days >= s.days()
                                || (s.id().equals("Desperto")
                                    && earnedAchievements.contains("FIRST_WORKOUT")),
                            s.id()
                                .equals(
                                    s.type().equals("TITLE")
                                        ? selected.titleId()
                                        : s.type().equals("FRAME")
                                            ? selected.frameId()
                                            : selected.itemId()),
                            s.id()))
                .toList());
    for (var legacy : LEGACY_TITLES) {
      boolean unlocked = earnedAchievements.contains(legacy.achievement());
      rewards.add(
          new Reward(
              legacy.id(),
              "TITLE",
              legacy.id(),
              "Um título do seu histórico de conquistas. O desbloqueio original foi preservado.",
              legacy.requirement(),
              unlocked ? 1 : 0,
              1,
              "conquista",
              unlocked,
              legacy.id().equals(selected.titleId()),
              legacy.id()));
    }
    long next =
        Arrays.stream(new int[] {1, 3, 7, 14, 30, 60, 90, 180})
            .filter(n -> n > days)
            .findFirst()
            .orElse((int) Math.min(Integer.MAX_VALUE, (days / 30 + 1) * 30));
    var step =
        new Step(
            "active-days-" + next,
            "Seu próximo pequeno passo",
            "Registre atividades em "
                + next
                + " dias diferentes. Descansos e pausas preservam o que você já conquistou.",
            days,
            next,
            "dias de atividade",
            "/check-in");
    return new Response(step, since != null && since >= 7, since, rewards, selected);
  }

  @Transactional
  public Response select(User user, Selection r) {
    // Same per-user lock also serializes initial preference creation.
    jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", UUID.class, user.getId());
    var state = response(user);
    String title = r.titleId() == null ? state.selected().titleId() : r.titleId();
    String frame = r.frameId() == null ? state.selected().frameId() : r.frameId();
    String item = r.itemId() == null ? state.selected().itemId() : r.itemId();
    validate(state, "TITLE", title, "");
    validate(state, "FRAME", frame, "none");
    validate(state, "ITEM", item, "none");
    int changed =
        jdbc.update(
            "UPDATE motivation_preferences SET title_id=?,frame_id=?,item_id=? WHERE user_id=?",
            title,
            frame,
            item,
            user.getId());
    if (changed == 0)
      jdbc.update(
          "INSERT INTO motivation_preferences(user_id,title_id,frame_id,item_id) VALUES(?,?,?,?)",
          user.getId(),
          title,
          frame,
          item);
    user.selectTitle(title);
    users.save(user);
    return response(user);
  }

  private static void validate(Response state, String type, String id, String empty) {
    if (id.equals(empty)) return;
    if (state.rewards().stream()
        .noneMatch(r -> r.type().equals(type) && r.id().equals(id) && r.unlocked()))
      throw new DomainConflictException(
          "Esta personalização ainda não está disponível. Continue no seu ritmo.");
  }

  private static boolean availableTitle(String title, long days, Set<String> achievements) {
    if (title == null || title.isEmpty()) return false;
    if (title.equals("Desperto")) return days >= 1 || achievements.contains("FIRST_WORKOUT");
    return SPECS.stream()
            .anyMatch(s -> s.type().equals("TITLE") && s.id().equals(title) && days >= s.days())
        || LEGACY_TITLES.stream()
            .anyMatch(s -> s.id().equals(title) && achievements.contains(s.achievement()));
  }

  private record Stats(long days, OffsetDateTime latest) {}
}
