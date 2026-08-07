package com.gymrats.api.ranking.application;

import static com.gymrats.api.ranking.api.RankingModels.*;

import com.gymrats.api.progression.domain.XpEventRepository;
import com.gymrats.api.ranking.domain.*;
import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.social.domain.GroupMemberRepository;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.UserRepository;
import com.gymrats.api.workout.domain.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingService {
  private final UserRepository users;
  private final XpEventRepository xpEvents;
  private final WorkoutSessionRepository sessions;
  private final GroupMemberRepository members;
  private final RankingSnapshotRepository snapshots;

  public RankingService(
      UserRepository users,
      XpEventRepository xpEvents,
      WorkoutSessionRepository sessions,
      GroupMemberRepository members,
      RankingSnapshotRepository snapshots) {
    this.users = users;
    this.xpEvents = xpEvents;
    this.sessions = sessions;
    this.members = members;
    this.snapshots = snapshots;
  }

  @Transactional
  public Result ranking(
      User current, String periodValue, String metricValue, UUID groupId, int requestedPage) {
    String period = normalize(periodValue, Set.of("WEEKLY", "MONTHLY"), "WEEKLY");
    String metric = normalize(metricValue, Set.of("XP", "CONSISTENCY"), "XP");
    var range = range(period);
    String scope = groupId == null ? "GLOBAL" : "GROUP";
    List<User> candidates;
    if (groupId == null) {
      candidates = users.findAll().stream()
          .filter(User::isActive)
          .filter(User::isRankingVisible)
          .toList();
    } else {
      if (!members.existsByGroupIdAndUserId(groupId, current.getId())) {
        throw new AccessDeniedException("Entre no grupo para consultar o ranking.");
      }
      candidates = members.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
          .map(value -> value.getUser())
          .filter(User::isActive)
          .filter(User::isRankingVisible)
          .toList();
    }
    var scores = candidates.stream()
        .map(user -> score(user, range, metric))
        .sorted(Comparator.comparingInt(Score::value).reversed()
            .thenComparing(Comparator.comparingInt(Score::days).reversed())
            .thenComparing(Comparator.comparingInt(Score::checkIns).reversed())
            .thenComparing(score -> score.user().getName(), String.CASE_INSENSITIVE_ORDER))
        .toList();
    var ranked = new ArrayList<Entry>();
    String snapshotKey = String.join(
        ":",
        scope,
        groupId == null ? "global" : groupId.toString(),
        period,
        metric,
        range.start().toString());
    for (int index = 0; index < scores.size(); index++) {
      var score = scores.get(index);
      int position = index + 1;
      ranked.add(new Entry(
          score.user().getId(),
          score.user().getName(),
          position,
          score.value(),
          score.user().getId().equals(current.getId())));
      var snapshot = snapshots.findBySnapshotKeyAndUserId(snapshotKey, score.user().getId())
          .orElseGet(() -> new RankingSnapshot(
              snapshotKey,
              score.user(),
              scope,
              groupId,
              period,
              metric,
              range.start(),
              range.end(),
              position,
              score.value(),
              score.days(),
              score.checkIns()));
      snapshot.refresh(position, score.value(), score.days(), score.checkIns());
      snapshots.save(snapshot);
    }
    int page = Math.max(1, requestedPage);
    int from = Math.min(ranked.size(), (page - 1) * 20);
    int to = Math.min(ranked.size(), from + 20);
    int totalPages = Math.max(1, (ranked.size() + 19) / 20);
    return new Result(
        period,
        metric,
        scope,
        range.start(),
        range.end(),
        metric.equals("XP")
            ? "XP validado; desempate por dias distintos e total bruto de check-ins."
            : "Dias distintos com check-in; desempate pelo total bruto de check-ins.",
        ranked.subList(from, to),
        page,
        totalPages);
  }

  private Score score(User user, Range range, String metric) {
    var completed =
        sessions.findByOwnerIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            user.getId(), WorkoutSessionStatus.COMPLETED, range.start(), range.end());
    int days = (int) completed.stream()
        .map(value -> value.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate())
        .distinct()
        .count();
    int checkIns = completed.size();
    int value = metric.equals("XP")
        ? Math.toIntExact(xpEvents.totalForUserBetween(user.getId(), range.start(), range.end()))
        : days;
    return new Score(user, value, days, checkIns);
  }

  private Range range(String period) {
    var today = LocalDate.now(ZoneOffset.UTC);
    LocalDate start =
        period.equals("MONTHLY")
            ? today.withDayOfMonth(1)
            : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate end = period.equals("MONTHLY") ? start.plusMonths(1) : start.plusDays(7);
    return new Range(
        start.atStartOfDay().toInstant(ZoneOffset.UTC),
        end.atStartOfDay().toInstant(ZoneOffset.UTC));
  }

  private String normalize(String value, Set<String> allowed, String fallback) {
    if (value == null || value.isBlank()) return fallback;
    String normalized = value.toUpperCase(Locale.ROOT);
    if (!allowed.contains(normalized)) {
      throw new DomainConflictException("Parâmetro de ranking inválido.");
    }
    return normalized;
  }
  private record Range(Instant start, Instant end) {}
  private record Score(User user, int value, int days, int checkIns) {}
}
