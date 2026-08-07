package com.gymrats.api.progression.application;

import com.gymrats.api.progression.api.AchievementResponse;
import com.gymrats.api.progression.api.ProgressionEventResponse;
import com.gymrats.api.progression.api.ProgressionOverviewResponse;
import com.gymrats.api.notification.application.NotificationService;
import com.gymrats.api.notification.domain.Notification;
import com.gymrats.api.progression.domain.UserAchievement;
import com.gymrats.api.progression.domain.UserAchievementRepository;
import com.gymrats.api.progression.domain.XpEvent;
import com.gymrats.api.progression.domain.XpEventRepository;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.UserRepository;
import com.gymrats.api.user.domain.WeeklyGoalVersionRepository;
import com.gymrats.api.workout.domain.WorkoutSession;
import com.gymrats.api.workout.domain.WorkoutSessionRepository;
import com.gymrats.api.workout.domain.WorkoutSessionStatus;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressionService {
  public static final int WORKOUT_XP = 50;
  private static final List<AchievementDefinition> DEFINITIONS =
      List.of(
          new AchievementDefinition(
              "FIRST_WORKOUT",
              "Primeiro passo",
              "Conclua seu primeiro treino.",
              "1 treino concluído",
              "Desperto"),
          new AchievementDefinition(
              "WEEKLY_THREE",
              "Consistência de Ferro",
              "Conclua treinos em três dias distintos da mesma semana.",
              "3 dias ativos na semana",
              "Vanguarda"),
          new AchievementDefinition(
              "XP_500",
              "Ascendente",
              "Acumule 500 XP validados.",
              "500 XP acumulados",
              "Ascendente"));

  private final XpEventRepository xpEvents;
  private final UserAchievementRepository achievements;
  private final WorkoutSessionRepository sessions;
  private final WeeklyGoalVersionRepository goals;
  private final UserRepository users;
  private final NotificationService notifications;
  private final AdvancedProgressionService advancedProgression;

  public ProgressionService(
      XpEventRepository xpEvents,
      UserAchievementRepository achievements,
      WorkoutSessionRepository sessions,
      WeeklyGoalVersionRepository goals,
      UserRepository users,
      NotificationService notifications,
      AdvancedProgressionService advancedProgression) {
    this.xpEvents = xpEvents;
    this.achievements = achievements;
    this.sessions = sessions;
    this.goals = goals;
    this.users = users;
    this.notifications = notifications;
    this.advancedProgression = advancedProgression;
  }

  @Transactional
  public void awardWorkout(User user, WorkoutSession session) {
    if (xpEvents
        .findByUserIdAndSourceTypeAndSourceId(
            user.getId(), "WORKOUT_CHECK_IN", session.getId())
        .isPresent()) {
      return;
    }
    var occurredAt =
        session.getCompletedAt() == null ? Instant.now() : session.getCompletedAt();
    xpEvents.save(
        new XpEvent(user, "WORKOUT_CHECK_IN", session.getId(), WORKOUT_XP, occurredAt));
    advancedProgression.awardWorkout(user, session, WORKOUT_XP, occurredAt);
    unlockEligible(user, occurredAt);
  }

  @Transactional(readOnly = true)
  public ProgressionOverviewResponse overview(User user) {
    long total = xpEvents.totalForUser(user.getId());
    var level = level(total);
    var period = currentWeek();
    long weeklyCompleted = activeDays(user.getId(), period.start(), period.end());
    int weeklyTarget =
        goals
            .findFirstByUserIdAndValidUntilIsNullOrderByValidFromDesc(user.getId())
            .map(value -> value.getTarget())
            .orElse(3);
    var rank = rank(total);
    var nextRank = nextRank(rank);
    return new ProgressionOverviewResponse(
        total,
        level.number(),
        level.progress(),
        level.requirement(),
        "Cada nível exige 100 × o nível atual de XP",
        weeklyTarget,
        weeklyCompleted,
        rank.name(),
        nextRank == null ? null : nextRank.name(),
        rankProgress(total, rank, nextRank));
  }

  @Transactional(readOnly = true)
  public List<ProgressionEventResponse> history(User user) {
    var history = new ArrayList<ProgressionEventResponse>();
    xpEvents.findByUserIdOrderByOccurredAtDesc(user.getId()).forEach(
        event ->
            history.add(
                new ProgressionEventResponse(
                    event.getId(),
                    "XP",
                    "+" + event.getAmount() + " XP",
                    "Check-in do treino",
                    event.getOccurredAt())));
    achievements.findByUserIdOrderByUnlockedAtDesc(user.getId()).forEach(
        unlocked -> {
          var definition = definition(unlocked.getCode());
          history.add(
              new ProgressionEventResponse(
                  unlocked.getId(),
                  "ACHIEVEMENT",
                  "Conquista " + definition.name() + " desbloqueada",
                  definition.criterion(),
                  unlocked.getUnlockedAt()));
        });
    history.sort(Comparator.comparing(ProgressionEventResponse::occurredAt).reversed());
    return history;
  }

  @Transactional(readOnly = true)
  public List<AchievementResponse> achievements(User user) {
    var unlocked =
        achievements.findByUserIdOrderByUnlockedAtDesc(user.getId()).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    UserAchievement::getCode, UserAchievement::getUnlockedAt));
    return DEFINITIONS.stream()
        .map(
            definition ->
                new AchievementResponse(
                    definition.code(),
                    definition.name(),
                    definition.description(),
                    definition.criterion(),
                    unlocked.get(definition.code()),
                    definition.title()))
        .toList();
  }

  @Transactional
  public String selectTitle(User user, String title) {
    String normalized = title == null ? "" : title.trim();
    if (!normalized.isEmpty()) {
      boolean unlocked =
          achievements.findByUserIdOrderByUnlockedAtDesc(user.getId()).stream()
              .map(value -> definition(value.getCode()).title())
              .anyMatch(normalized::equals);
      if (!unlocked) {
        throw new AccessDeniedException("Selecione apenas um título desbloqueado.");
      }
    }
    user.selectTitle(normalized);
    users.save(user);
    return normalized;
  }

  private void unlockEligible(User user, Instant now) {
    long total = xpEvents.totalForUser(user.getId());
    var week = currentWeek();
    long days = activeDays(user.getId(), week.start(), week.end());
    unlock(user, "FIRST_WORKOUT", total >= WORKOUT_XP, now);
    unlock(user, "WEEKLY_THREE", days >= 3, now);
    unlock(user, "XP_500", total >= 500, now);
  }

  private void unlock(User user, String code, boolean eligible, Instant now) {
    if (eligible && !achievements.existsByUserIdAndCode(user.getId(), code)) {
      achievements.save(new UserAchievement(user, code, now));
      var unlocked = definition(code);
      notifications.emit(
          user,
          "achievement:" + code,
          Notification.Kind.ACHIEVEMENT,
          "Nova conquista",
          unlocked.name() + " foi desbloqueada.",
          "/progression");
    }
  }

  private long activeDays(UUID userId, Instant start, Instant end) {
    return sessions
        .findByOwnerIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            userId, WorkoutSessionStatus.COMPLETED, start, end)
        .stream()
        .map(value -> value.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate())
        .distinct()
        .count();
  }

  private Period currentWeek() {
    var today = LocalDate.now(ZoneOffset.UTC);
    var start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return new Period(
        start.atStartOfDay().toInstant(ZoneOffset.UTC),
        start.plusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC));
  }

  private Level level(long totalXp) {
    int number = 1;
    long remaining = totalXp;
    long requirement = 100;
    while (remaining >= requirement) {
      remaining -= requirement;
      number++;
      requirement = 100L * number;
    }
    return new Level(number, remaining, requirement);
  }

  private Rank rank(long totalXp) {
    if (totalXp >= 7000) return Rank.A;
    if (totalXp >= 3500) return Rank.B;
    if (totalXp >= 1500) return Rank.C;
    if (totalXp >= 500) return Rank.D;
    return Rank.E;
  }

  private Rank nextRank(Rank rank) {
    return switch (rank) {
      case E -> Rank.D;
      case D -> Rank.C;
      case C -> Rank.B;
      case B -> Rank.A;
      case A -> null;
    };
  }

  private int rankProgress(long total, Rank rank, Rank next) {
    if (next == null) return 100;
    long currentFloor = rank.threshold;
    long target = next.threshold;
    return (int) Math.min(100, Math.max(0, (total - currentFloor) * 100 / (target - currentFloor)));
  }

  private AchievementDefinition definition(String code) {
    return DEFINITIONS.stream()
        .filter(value -> value.code().equals(code))
        .findFirst()
        .orElseThrow();
  }

  private enum Rank {
    E(0),
    D(500),
    C(1500),
    B(3500),
    A(7000);
    private final long threshold;
    Rank(long threshold) { this.threshold = threshold; }
  }

  private record Level(int number, long progress, long requirement) {}
  private record Period(Instant start, Instant end) {}
  private record AchievementDefinition(
      String code, String name, String description, String criterion, String title) {}
}
