package com.gymrats.api.progression.application;

import com.gymrats.api.progression.api.HallOfFameResponse;
import com.gymrats.api.progression.api.HallOfFameResponse.HallOfFameEntryResponse;
import com.gymrats.api.progression.api.HallOfFameResponse.MonarchResponse;
import com.gymrats.api.progression.api.HallOfFameResponse.ReignResponse;
import com.gymrats.api.progression.domain.AdvancedRank;
import com.gymrats.api.progression.domain.AdvancedRankProfile;
import com.gymrats.api.progression.domain.AdvancedRankProfileRepository;
import com.gymrats.api.progression.domain.MonarchReign;
import com.gymrats.api.progression.domain.MonarchReignRepository;
import com.gymrats.api.progression.domain.MonarchStateRepository;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.workout.domain.WorkoutSessionRepository;
import com.gymrats.api.workout.domain.WorkoutSessionStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonarchService {
  public static final String CALCULATION =
      "Somente Nacionais ativos e visíveis: mais dias distintos com check-in; "
          + "depois, total de check-ins.";

  private final AdvancedRankProfileRepository rankProfiles;
  private final WorkoutSessionRepository sessions;
  private final MonarchStateRepository states;
  private final MonarchReignRepository reigns;

  public MonarchService(
      AdvancedRankProfileRepository rankProfiles,
      WorkoutSessionRepository sessions,
      MonarchStateRepository states,
      MonarchReignRepository reigns) {
    this.rankProfiles = rankProfiles;
    this.sessions = sessions;
    this.states = states;
    this.reigns = reigns;
  }

  @Transactional
  public void reconcile(Instant now) {
    var state = states.lockSingleton().orElseThrow();
    User winner = eligibleWinner();
    MonarchReign current = state.getCurrentReign();
    if (current != null
        && winner != null
        && current.getUser().getId().equals(winner.getId())) {
      return;
    }

    if (current != null) {
      current.end(now);
      reigns.saveAndFlush(current);
    }

    MonarchReign next = null;
    if (winner != null) {
      next = reigns.save(new MonarchReign(winner, now));
    }
    state.assign(next, now);
    states.save(state);
  }

  @Transactional
  public HallOfFameResponse hallOfFame(User currentUser) {
    Instant now = Instant.now();
    reconcile(now);
    var state = states.lockSingleton().orElseThrow();
    List<MonarchReign> history = reigns.findAllByOrderByStartedAtDesc();
    MonarchResponse current =
        state.getCurrentReign() == null
            ? null
            : monarch(state.getCurrentReign(), currentUser, now);

    Map<UUID, List<MonarchReign>> byUser = new LinkedHashMap<>();
    history.forEach(
        reign ->
            byUser.computeIfAbsent(reign.getUser().getId(), ignored -> new ArrayList<>())
                .add(reign));
    List<HallOfFameEntryResponse> entries =
        byUser.values().stream()
            .map(userReigns -> entry(userReigns, currentUser, now))
            .sorted(
                Comparator.comparingInt(HallOfFameEntryResponse::reignCount)
                    .reversed()
                    .thenComparing(
                        Comparator.comparingLong(
                                HallOfFameEntryResponse::longestReignSeconds)
                            .reversed())
                    .thenComparing(
                        HallOfFameEntryResponse::displayName,
                        String.CASE_INSENSITIVE_ORDER))
            .toList();
    return new HallOfFameResponse(current, CALCULATION, entries);
  }

  private User eligibleWinner() {
    return rankProfiles.findByCurrentRank(AdvancedRank.NATIONAL).stream()
        .filter(profile -> profile.getUser().isActive())
        .filter(profile -> profile.getUser().isRankingVisible())
        .map(this::score)
        .sorted(
            Comparator.comparingLong(NationalScore::activeDays)
                .reversed()
                .thenComparing(
                    Comparator.comparingInt(NationalScore::checkIns).reversed())
                .thenComparing(
                    NationalScore::nationalSince,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(score -> score.user().getId()))
        .map(NationalScore::user)
        .findFirst()
        .orElse(null);
  }

  private NationalScore score(AdvancedRankProfile profile) {
    var completed =
        sessions.findAllByOwnerIdAndStatus(
            profile.getUser().getId(), WorkoutSessionStatus.COMPLETED);
    long activeDays =
        completed.stream()
            .map(
                session ->
                    session.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate())
            .distinct()
            .count();
    return new NationalScore(
        profile.getUser(), activeDays, completed.size(), profile.getNationalSince());
  }

  private MonarchResponse monarch(
      MonarchReign reign, User currentUser, Instant now) {
    return new MonarchResponse(
        reign.getUser().getId(),
        reign.getUser().getName(),
        reign.getStartedAt(),
        durationSeconds(reign, now),
        reign.getUser().getId().equals(currentUser.getId()));
  }

  private HallOfFameEntryResponse entry(
      List<MonarchReign> userReigns, User currentUser, Instant now) {
    MonarchReign latest = userReigns.get(0);
    long longest =
        userReigns.stream()
            .mapToLong(reign -> durationSeconds(reign, now))
            .max()
            .orElse(0);
    long total =
        userReigns.stream().mapToLong(reign -> durationSeconds(reign, now)).sum();
    Instant first =
        userReigns.stream()
            .map(MonarchReign::getStartedAt)
            .min(Comparator.naturalOrder())
            .orElseThrow();
    boolean current = userReigns.stream().anyMatch(MonarchReign::isActive);
    List<ReignResponse> periods =
        userReigns.stream()
            .map(
                reign ->
                    new ReignResponse(
                        reign.getId(),
                        reign.getStartedAt(),
                        reign.getEndedAt(),
                        durationSeconds(reign, now),
                        reign.isActive()))
            .toList();
    return new HallOfFameEntryResponse(
        latest.getUser().getId(),
        latest.getUser().getName(),
        userReigns.size(),
        longest,
        total,
        first,
        latest.getStartedAt(),
        current,
        latest.getUser().getId().equals(currentUser.getId()),
        periods);
  }

  private long durationSeconds(MonarchReign reign, Instant now) {
    Instant end = reign.getEndedAt() == null ? now : reign.getEndedAt();
    return Math.max(0, Duration.between(reign.getStartedAt(), end).getSeconds());
  }

  private record NationalScore(
      User user, long activeDays, int checkIns, Instant nationalSince) {}
}
