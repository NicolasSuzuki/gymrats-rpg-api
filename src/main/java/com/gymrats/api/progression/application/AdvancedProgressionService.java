package com.gymrats.api.progression.application;

import com.gymrats.api.progression.api.AdvancedProgressionResponse;
import com.gymrats.api.progression.api.AdvancedProgressionResponse.AdvancedRankEventResponse;
import com.gymrats.api.progression.api.AdvancedProgressionResponse.ClassMasteryEventResponse;
import com.gymrats.api.progression.api.AdvancedProgressionResponse.ClassMasteryResponse;
import com.gymrats.api.progression.domain.AdvancedRank;
import com.gymrats.api.progression.domain.AdvancedRankEvent;
import com.gymrats.api.progression.domain.AdvancedRankEventRepository;
import com.gymrats.api.progression.domain.AdvancedRankProfile;
import com.gymrats.api.progression.domain.AdvancedRankProfileRepository;
import com.gymrats.api.progression.domain.ClassMasteryEvent;
import com.gymrats.api.progression.domain.ClassMasteryEventRepository;
import com.gymrats.api.progression.domain.XpEventRepository;
import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.user.domain.CharacterClass;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.WeeklyGoalVersion;
import com.gymrats.api.user.domain.WeeklyGoalVersionRepository;
import com.gymrats.api.workout.domain.WorkoutSession;
import com.gymrats.api.workout.domain.WorkoutSessionRepository;
import com.gymrats.api.workout.domain.WorkoutSessionStatus;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvancedProgressionService {
  private static final long BASE_RANK_A_XP = 7000;
  private static final int MASTERY_LEVEL_XP = 500;
  private static final int NATIONAL_EVALUATION_DAYS = 30;

  private final ClassMasteryEventRepository masteryEvents;
  private final AdvancedRankProfileRepository rankProfiles;
  private final AdvancedRankEventRepository rankEvents;
  private final XpEventRepository xpEvents;
  private final WorkoutSessionRepository sessions;
  private final WeeklyGoalVersionRepository goals;
  private final int s5Weeks;
  private final int s4Weeks;
  private final int s3Weeks;
  private final int s2Weeks;
  private final int s1Weeks;
  private final int protectionDays;
  private final String tieBreaker;
  private final MonarchService monarchs;

  public AdvancedProgressionService(
      ClassMasteryEventRepository masteryEvents,
      AdvancedRankProfileRepository rankProfiles,
      AdvancedRankEventRepository rankEvents,
      XpEventRepository xpEvents,
      WorkoutSessionRepository sessions,
      WeeklyGoalVersionRepository goals,
      @Value("${app.progression.advanced.s5-weeks:4}") int s5Weeks,
      @Value("${app.progression.advanced.s4-weeks:8}") int s4Weeks,
      @Value("${app.progression.advanced.s3-weeks:12}") int s3Weeks,
      @Value("${app.progression.advanced.s2-weeks:16}") int s2Weeks,
      @Value("${app.progression.advanced.s1-weeks:20}") int s1Weeks,
      @Value("${app.progression.advanced.protection-days:7}") int protectionDays,
      @Value(
              "${app.progression.advanced.tie-breaker:"
                  + "DISTINCT_ACTIVE_DAYS_THEN_CHECK_INS}")
          String tieBreaker,
      MonarchService monarchs) {
    this.masteryEvents = masteryEvents;
    this.rankProfiles = rankProfiles;
    this.rankEvents = rankEvents;
    this.xpEvents = xpEvents;
    this.sessions = sessions;
    this.goals = goals;
    this.s5Weeks = s5Weeks;
    this.s4Weeks = s4Weeks;
    this.s3Weeks = s3Weeks;
    this.s2Weeks = s2Weeks;
    this.s1Weeks = s1Weeks;
    this.protectionDays = protectionDays;
    this.tieBreaker = tieBreaker;
    this.monarchs = monarchs;
  }

  @Transactional
  public void awardWorkout(
      User user,
      WorkoutSession session,
      int amount,
      Instant occurredAt) {
    if (user.getCharacterClass() != null
        && !masteryEvents.existsByUserIdAndCharacterClassAndSourceId(
            user.getId(), user.getCharacterClass(), session.getId())) {
      masteryEvents.save(
          new ClassMasteryEvent(
              user,
              user.getCharacterClass(),
              session.getId(),
              amount,
              occurredAt));
    }
    evaluateRank(user, occurredAt);
    monarchs.reconcile(occurredAt);
  }

  @Transactional
  public AdvancedProgressionResponse updatePause(
      User user,
      boolean paused,
      String reason) {
    if (xpEvents.totalForUser(user.getId()) < BASE_RANK_A_XP) {
      throw new DomainConflictException(
          "A pausa protegida fica disponível a partir do rank A.");
    }
    Instant now = Instant.now();
    var profile =
        rankProfiles.findById(user.getId()).orElseGet(() -> new AdvancedRankProfile(user));
    if (paused) {
      if (!profile.isPaused()) {
        String normalized =
            reason == null || reason.isBlank()
                ? "Pausa protegida solicitada pelo usuário."
                : reason.trim();
        profile.pause(normalized, now);
        rankEvents.save(
            new AdvancedRankEvent(
                user,
                profile.getCurrentRank(),
                profile.getCurrentRank(),
                "Avaliação congelada: " + normalized,
                now));
      }
    } else if (profile.isPaused()) {
      profile.resume(now);
      rankEvents.save(
          new AdvancedRankEvent(
              user,
              profile.getCurrentRank(),
              profile.getCurrentRank(),
              "Avaliação retomada.",
              now));
    }
    rankProfiles.save(profile);
    return response(user, now);
  }

  @Transactional(readOnly = true)
  public AdvancedProgressionResponse overview(User user) {
    return response(user, Instant.now());
  }

  private void evaluateRank(User user, Instant now) {
    if (xpEvents.totalForUser(user.getId()) < BASE_RANK_A_XP) return;
    var profile =
        rankProfiles.findById(user.getId()).orElseGet(() -> rankProfiles.save(
            new AdvancedRankProfile(user)));
    if (profile.isPaused()) return;

    int weeks = consistentWeeks(user, s1Weeks);
    AdvancedRank current = profile.getCurrentRank();
    if (current == AdvancedRank.NATIONAL && weeks < s1Weeks) {
      transition(user, profile, AdvancedRank.S1, "Consistência mínima perdida.", now);
      return;
    }

    AdvancedRank eligible = eligibleRank(weeks);
    if (eligible.ordinal() > current.ordinal()) {
      transition(
          user,
          profile,
          eligible,
          "Critério de " + weeks + " semana(s) consistente(s) atingido.",
          now);
      current = eligible;
    } else if (eligible.ordinal() < current.ordinal()
        && current != AdvancedRank.NATIONAL
        && !isProtected(profile, now)) {
      AdvancedRank previous = AdvancedRank.values()[current.ordinal() - 1];
      transition(
          user,
          profile,
          previous,
          "Consistência abaixo do critério do sub-rank.",
          now);
      current = previous;
    }

    if (current == AdvancedRank.S1
        && assessmentDays(profile, now) >= NATIONAL_EVALUATION_DAYS) {
      transition(
          user,
          profile,
          AdvancedRank.NATIONAL,
          "Avaliação de 30 dias em S-1 concluída.",
          now);
    }
  }

  private void transition(
      User user,
      AdvancedRankProfile profile,
      AdvancedRank next,
      String reason,
      Instant now) {
    AdvancedRank previous = profile.getCurrentRank();
    if (previous == next) return;
    profile.transition(next, now, now.plus(Duration.ofDays(protectionDays)));
    rankProfiles.save(profile);
    rankEvents.save(new AdvancedRankEvent(user, previous, next, reason, now));
  }

  private AdvancedProgressionResponse response(User user, Instant now) {
    long totalXp = xpEvents.totalForUser(user.getId());
    boolean eligible = totalXp >= BASE_RANK_A_XP;
    var profile = eligible ? rankProfiles.findById(user.getId()).orElse(null) : null;
    AdvancedRank current = profile == null ? AdvancedRank.A : profile.getCurrentRank();
    int weeks = consistentWeeks(user, s1Weeks);
    AdvancedRank next = nextRank(current);
    int requirement = next == null ? s1Weeks : requiredWeeks(next);
    int floor = requiredWeeks(current);
    int progress =
        next == null
            ? 100
            : current == AdvancedRank.S1
                ? assessmentDays(profile, now) * 100 / NATIONAL_EVALUATION_DAYS
                : Math.min(
                    100,
                    Math.max(
                        0,
                        (weeks - floor) * 100 / Math.max(1, requirement - floor)));

    List<ClassMasteryResponse> masteries =
        Arrays.stream(CharacterClass.values())
            .map(
                characterClass -> {
                  long xp = masteryEvents.totalForClass(user.getId(), characterClass);
                  return new ClassMasteryResponse(
                      characterClass.name(),
                      xp,
                      (int) (xp / MASTERY_LEVEL_XP) + 1,
                      xp % MASTERY_LEVEL_XP,
                      MASTERY_LEVEL_XP,
                      characterClass == user.getCharacterClass());
                })
            .toList();
    List<ClassMasteryEventResponse> masteryHistory =
        masteryEvents.findTop20ByUserIdOrderByOccurredAtDesc(user.getId()).stream()
            .map(
                event ->
                    new ClassMasteryEventResponse(
                        event.getId(),
                        event.getCharacterClass().name(),
                        event.getAmount(),
                        event.getOccurredAt()))
            .toList();
    List<AdvancedRankEventResponse> history =
        rankEvents.findTop20ByUserIdOrderByOccurredAtDesc(user.getId()).stream()
            .map(
                event ->
                    new AdvancedRankEventResponse(
                        event.getId(),
                        event.getPreviousRank().displayName(),
                        event.getNewRank().displayName(),
                        event.getReason(),
                        event.getOccurredAt()))
            .toList();

    return new AdvancedProgressionResponse(
        user.getCharacterClass() == null ? null : user.getCharacterClass().name(),
        masteries,
        masteryHistory,
        eligible,
        eligible ? current.displayName() : baseRank(totalXp),
        eligible && next != null ? next.displayName() : eligible ? null : "A",
        weeks,
        requirement,
        eligible ? progress : (int) Math.min(100, totalXp * 100 / BASE_RANK_A_XP),
        profile == null ? null : profile.getProtectedUntil(),
        profile != null && profile.isPaused(),
        profile == null ? null : profile.getPauseStartedAt(),
        profile == null ? null : profile.getPauseReason(),
        profile == null ? 0 : assessmentDays(profile, now),
        NATIONAL_EVALUATION_DAYS,
        tieBreaker,
        history);
  }

  private int consistentWeeks(User user, int limit) {
    LocalDate currentStart =
        LocalDate.now(ZoneOffset.UTC)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    List<WeeklyGoalVersion> versions =
        goals.findByUserIdOrderByValidFromDesc(user.getId());
    int count = 0;
    LocalDate cursor = currentStart;
    int currentTarget = targetForWeek(versions, cursor);
    if (activeDays(user, cursor) >= currentTarget) {
      count++;
    }
    cursor = cursor.minusWeeks(1);
    while (count < limit) {
      int target = targetForWeek(versions, cursor);
      if (activeDays(user, cursor) < target) break;
      count++;
      cursor = cursor.minusWeeks(1);
    }
    return count;
  }

  private int targetForWeek(List<WeeklyGoalVersion> versions, LocalDate start) {
    Instant weekStart = start.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant weekEnd = start.plusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);
    return versions.stream()
        .filter(version -> version.getValidFrom().isBefore(weekEnd))
        .filter(
            version ->
                version.getValidUntil() == null
                    || !version.getValidUntil().isBefore(weekStart))
        .findFirst()
        .map(WeeklyGoalVersion::getTarget)
        .orElse(3);
  }

  private long activeDays(User user, LocalDate start) {
    Instant from = start.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant to = start.plusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);
    return sessions
        .findByOwnerIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            user.getId(), WorkoutSessionStatus.COMPLETED, from, to)
        .stream()
        .map(session -> session.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate())
        .distinct()
        .count();
  }

  private AdvancedRank eligibleRank(int weeks) {
    if (weeks >= s1Weeks) return AdvancedRank.S1;
    if (weeks >= s2Weeks) return AdvancedRank.S2;
    if (weeks >= s3Weeks) return AdvancedRank.S3;
    if (weeks >= s4Weeks) return AdvancedRank.S4;
    if (weeks >= s5Weeks) return AdvancedRank.S5;
    return AdvancedRank.A;
  }

  private int requiredWeeks(AdvancedRank rank) {
    return switch (rank) {
      case A -> 0;
      case S5 -> s5Weeks;
      case S4 -> s4Weeks;
      case S3 -> s3Weeks;
      case S2 -> s2Weeks;
      case S1, NATIONAL -> s1Weeks;
    };
  }

  private AdvancedRank nextRank(AdvancedRank rank) {
    return switch (rank) {
      case A -> AdvancedRank.S5;
      case S5 -> AdvancedRank.S4;
      case S4 -> AdvancedRank.S3;
      case S3 -> AdvancedRank.S2;
      case S2 -> AdvancedRank.S1;
      case S1 -> AdvancedRank.NATIONAL;
      case NATIONAL -> null;
    };
  }

  private int assessmentDays(AdvancedRankProfile profile, Instant now) {
    if (profile.getS1Since() == null) return 0;
    long paused = profile.getS1PausedSeconds();
    if (profile.getPauseStartedAt() != null) {
      paused +=
          Math.max(0, Duration.between(profile.getPauseStartedAt(), now).getSeconds());
    }
    long activeSeconds =
        Math.max(0, Duration.between(profile.getS1Since(), now).getSeconds() - paused);
    return (int) Math.min(
        NATIONAL_EVALUATION_DAYS, activeSeconds / Duration.ofDays(1).getSeconds());
  }

  private boolean isProtected(AdvancedRankProfile profile, Instant now) {
    return profile.getProtectedUntil() != null
        && profile.getProtectedUntil().isAfter(now);
  }

  private String baseRank(long totalXp) {
    if (totalXp >= BASE_RANK_A_XP) return "A";
    if (totalXp >= 3500) return "B";
    if (totalXp >= 1500) return "C";
    if (totalXp >= 500) return "D";
    return "E";
  }
}
