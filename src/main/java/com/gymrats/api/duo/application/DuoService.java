package com.gymrats.api.duo.application;

import com.gymrats.api.duo.api.DuoModels.*;
import com.gymrats.api.feed.application.MediaStorage;
import com.gymrats.api.feed.domain.WorkoutMediaRepository;
import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.shared.application.ResourceNotFoundException;
import com.gymrats.api.user.domain.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DuoService {
  public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  private final JdbcTemplate jdbc;
  private final MediaStorage storage;
  private final WorkoutMediaRepository workoutMedia;

  public DuoService(JdbcTemplate jdbc, MediaStorage storage, WorkoutMediaRepository workoutMedia) {
    this.jdbc = jdbc;
    this.storage = storage;
    this.workoutMedia = workoutMedia;
  }

  @Transactional(readOnly = true)
  public Panel panel(User user) {
    LocalDate today = LocalDate.now(ZONE), week = week(today);
    UUID duo = activeId(user.getId());
    if (duo == null) {
      var invitation = currentInvite(user.getId());
      return new Panel(
          invitation == null ? "NONE" : "WAITING",
          null,
          week,
          List.of(),
          new Cooperative(false, 0),
          invitation,
          List.of(),
          List.of(),
          new Settings(true, true),
          List.of(),
          new MonthlyRecap(YearMonth.from(today).toString(), 0, 0, 0, List.of()));
    }
    var people = people(duo, user.getId());
    var days =
        activeDays(
            people.stream().map(Person::id).toList(),
            today.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            today.plusDays(1));
    var goals = goals(people.stream().map(Person::id).toList());
    var members =
        people.stream()
            .map(
                p -> {
                  int target = target(goals, p.id(), Instant.now());
                  long count = count(days, p.id(), week, today.plusDays(1));
                  return new Member(
                      p.id(),
                      p.name(),
                      p.id().equals(user.getId()),
                      target,
                      count,
                      Math.min(1d, (double) count / target));
                })
            .toList();
    double progress = members.stream().mapToDouble(Member::ratio).average().orElse(0);
    var settings =
        jdbc.queryForObject(
            "SELECT reminders_enabled,encouragement_enabled FROM duo_members WHERE duo_id=? AND"
                + " active_user_id=?",
            (rs, n) -> new Settings(rs.getBoolean(1), rs.getBoolean(2)),
            duo,
            user.getId());
    Instant meetingListStart = Instant.now();
    var meetings =
        jdbc.query(
            """
            SELECT id,proposer_id,scheduled_at,location,note,status FROM duo_meetings
            WHERE duo_id=?
            ORDER BY CASE WHEN scheduled_at>? AND status<>'DECLINED' THEN 0 ELSE 1 END,
              CASE WHEN scheduled_at>? AND status<>'DECLINED' THEN scheduled_at END ASC,
              scheduled_at DESC,id DESC LIMIT 30
            """,
            (rs, n) ->
                new Meeting(
                    uuid(rs, "id"),
                    uuid(rs, "proposer_id"),
                    time(rs, "scheduled_at"),
                    rs.getString("location"),
                    rs.getString("note"),
                    rs.getString("status"),
                    rs.getString("status").equals("PENDING")
                        && !uuid(rs, "proposer_id").equals(user.getId())
                        && time(rs, "scheduled_at").isAfter(Instant.now()),
                    !rs.getString("status").equals("DECLINED")),
            duo,
            ts(meetingListStart),
            ts(meetingListStart));
    // Reminders are independent of the limited meeting history: distant events cannot hide
    // tomorrow.
    Instant reminderStart = Instant.now();
    var reminders =
        settings.remindersEnabled()
            ? jdbc.query(
                """
                SELECT id,scheduled_at FROM duo_meetings
                WHERE duo_id=? AND status='ACCEPTED' AND scheduled_at>? AND scheduled_at<?
                ORDER BY scheduled_at,id LIMIT 30
                """,
                (rs, n) ->
                    new Reminder(
                        uuid(rs, "id"),
                        "Vocês combinaram uma atividade. Ajustem o horário se precisarem.",
                        time(rs, "scheduled_at")),
                duo,
                ts(reminderStart),
                ts(reminderStart.plus(Duration.ofDays(7))))
            : List.<Reminder>of();
    var created =
        jdbc.queryForObject(
                "SELECT created_at FROM duo_partnerships WHERE id=?",
                (rs, n) -> time(rs, "created_at"),
                duo)
            .atZone(ZONE)
            .toLocalDate();
    LocalDate monthStart = today.withDayOfMonth(1),
        countStart = created.isAfter(monthStart) ? created : monthStart;
    var monthly =
        people.stream()
            .map(
                p ->
                    new MonthlyMember(
                        p.id(), p.name(), count(days, p.id(), countStart, today.plusDays(1))))
            .toList();
    long cooperativeWeeks = 0;
    for (LocalDate start = week(monthStart);
        start.plusWeeks(1).compareTo(today) <= 0;
        start = start.plusWeeks(1)) {
      if (start.isBefore(week(created))) continue;
      Instant boundary = start.plusWeeks(1).atStartOfDay(ZONE).toInstant().minusNanos(1);
      final LocalDate from = start;
      if (people.stream()
          .allMatch(
              p -> count(days, p.id(), from, from.plusWeeks(1)) >= target(goals, p.id(), boundary)))
        cooperativeWeeks++;
    }
    long memories =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM duo_memories WHERE duo_id=? AND created_at>=?",
            Long.class,
            duo,
            ts(monthStart.atStartOfDay(ZONE).toInstant()));
    return new Panel(
        "ACTIVE",
        duo,
        week,
        members,
        new Cooperative(
            members.size() == 2 && members.stream().allMatch(m -> m.ratio() >= 1), progress),
        null,
        meetings,
        memories(duo, user.getId(), settings.encouragementEnabled()),
        settings,
        reminders,
        new MonthlyRecap(
            YearMonth.from(today).toString(),
            monthly.stream().mapToLong(MonthlyMember::activeDays).sum(),
            memories,
            cooperativeWeeks,
            monthly));
  }

  @Transactional
  public Invitation invite(User user) {
    lockUsers(user.getId());
    if (activeId(user.getId()) != null) throw conflict("Você já possui uma parceria ativa.");
    var existing = currentInvite(user.getId());
    if (existing != null) return existing;
    jdbc.update("DELETE FROM duo_invites WHERE issuer_id=?", user.getId());
    String token =
        UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "");
    Instant expires = Instant.now().plus(Duration.ofDays(7));
    jdbc.update(
        "INSERT INTO duo_invites(token,issuer_id,expires_at) VALUES(?,?,?)",
        token,
        user.getId(),
        ts(expires));
    return new Invitation(token, expires);
  }

  @Transactional
  public void cancelInvite(User user) {
    lockUsers(user.getId());
    jdbc.update("DELETE FROM duo_invites WHERE issuer_id=? AND duo_id IS NULL", user.getId());
  }

  @Transactional(readOnly = true)
  public InvitePreview preview(User user, String token) {
    var invite = findInvite(token, false);
    boolean available =
        invite.duo() == null
            && invite.expires().isAfter(Instant.now())
            && !invite.issuer().equals(user.getId())
            && activeId(invite.issuer()) == null
            && activeId(user.getId()) == null;
    String name =
        jdbc.queryForObject(
            "SELECT name FROM users WHERE id=? AND active=TRUE", String.class, invite.issuer());
    return new InvitePreview(available, name, invite.expires());
  }

  @Transactional
  public Panel join(User user, String token) {
    var initial = findInvite(token, false);
    if (initial.issuer().equals(user.getId()))
      throw conflict("Compartilhe este convite com outra pessoa.");
    lockUsers(initial.issuer(), user.getId());
    var invite = findInvite(token, true);
    if (invite.duo() != null) {
      if (invite.duo().equals(activeId(user.getId()))) return panel(user);
      throw conflict("Este convite já foi utilizado.");
    }
    if (!invite.expires().isAfter(Instant.now()))
      throw conflict("Este convite expirou. Peça um novo link.");
    if (activeId(user.getId()) != null || activeId(invite.issuer()) != null)
      throw conflict("Uma das pessoas já possui uma parceria ativa.");
    UUID duo = UUID.randomUUID();
    Instant now = Instant.now();
    jdbc.update(
        "INSERT INTO duo_partnerships(id,status,created_at) VALUES(?,'ACTIVE',?)", duo, ts(now));
    for (UUID id : List.of(invite.issuer(), user.getId()))
      jdbc.update(
          "INSERT INTO duo_members(duo_id,user_id,active_user_id,joined_at) VALUES(?,?,?,?)",
          duo,
          id,
          id,
          ts(now));
    jdbc.update("UPDATE duo_invites SET duo_id=? WHERE token=?", duo, token);
    // A person joining a partnership revokes all other pending invitations they had shared.
    jdbc.update(
        "DELETE FROM duo_invites WHERE issuer_id IN (?,?) AND duo_id IS NULL",
        invite.issuer(),
        user.getId());
    return panel(user);
  }

  @Transactional
  public void leave(User user) {
    UUID duo = activeId(user.getId());
    if (duo != null) {
      duo = requireActive(user, true);
      var keys =
          jdbc.queryForList(
              "SELECT storage_key FROM duo_private_media WHERE duo_id=?", String.class, duo);
      jdbc.update(
          "UPDATE duo_partnerships SET status='ENDED',ended_at=? WHERE id=?",
          ts(Instant.now()),
          duo);
      jdbc.update("UPDATE duo_members SET active_user_id=NULL WHERE duo_id=?", duo);
      jdbc.update("DELETE FROM duo_memories WHERE duo_id=?", duo);
      jdbc.update("DELETE FROM duo_private_media WHERE duo_id=?", duo);
      jdbc.update("DELETE FROM duo_meetings WHERE duo_id=?", duo);
      jdbc.update(
          "DELETE FROM duo_invites WHERE issuer_id IN (SELECT user_id FROM duo_members WHERE"
              + " duo_id=?) AND duo_id IS NULL",
          duo);
      jdbc.update("DELETE FROM duo_invites WHERE duo_id=?", duo);
      keys.forEach(this::deleteAfterCommit);
    }
    jdbc.update("DELETE FROM duo_invites WHERE issuer_id=?", user.getId());
  }

  @Transactional
  public Panel propose(User user, MeetingRequest request) {
    UUID duo = requireActive(user, true);
    validateFuture(request.scheduledAt());
    var existing =
        jdbc.queryForList(
            "SELECT id FROM duo_meetings WHERE duo_id=? AND proposer_id=? AND scheduled_at=?",
            UUID.class,
            duo,
            user.getId(),
            ts(request.scheduledAt()));
    if (existing.isEmpty())
      jdbc.update(
          "INSERT INTO"
              + " duo_meetings(id,duo_id,proposer_id,scheduled_at,location,note,status,created_at)"
              + " VALUES(?,?,?,?,?,?,'PENDING',?)",
          UUID.randomUUID(),
          duo,
          user.getId(),
          ts(request.scheduledAt()),
          clean(request.location()),
          clean(request.note()),
          ts(Instant.now()));
    return panel(user);
  }

  @Transactional
  public Panel meeting(User user, UUID id, MeetingChange request) {
    UUID duo = requireActive(user, true);
    var rows =
        jdbc.query(
            "SELECT proposer_id,status,scheduled_at FROM duo_meetings WHERE duo_id=? AND id=?",
            (rs, n) ->
                new MeetingState(
                    uuid(rs, "proposer_id"), rs.getString("status"), time(rs, "scheduled_at")),
            duo,
            id);
    if (rows.isEmpty()) throw missing();
    var state = rows.get(0);
    if (request.action() == MeetingAction.RESCHEDULE) {
      if (state.status().equals("DECLINED"))
        throw conflict("Crie um novo convite para esta atividade.");
      validateFuture(request.scheduledAt());
      jdbc.update(
          "UPDATE duo_meetings SET proposer_id=?,scheduled_at=?,location=?,note=?,status='PENDING'"
              + " WHERE id=?",
          user.getId(),
          ts(request.scheduledAt()),
          clean(request.location()),
          clean(request.note()),
          id);
    } else {
      String next = request.action() == MeetingAction.ACCEPT ? "ACCEPTED" : "DECLINED";
      if (state.proposer().equals(user.getId()))
        throw new AccessDeniedException("A outra pessoa deve responder ao convite.");
      if (request.expectedScheduledAt() == null
          || !state.scheduled().equals(request.expectedScheduledAt()))
        throw conflict(
            "O horário deste convite mudou. Confira os novos detalhes antes de responder.");
      if (!state.status().equals(next)) {
        if (!state.status().equals("PENDING") || !state.scheduled().isAfter(Instant.now()))
          throw conflict("Este convite não pode mais receber esta resposta.");
        jdbc.update("UPDATE duo_meetings SET status=? WHERE id=?", next, id);
      }
    }
    return panel(user);
  }

  @Transactional(readOnly = true)
  public List<Workout> workouts(User user) {
    UUID duo = activeId(user.getId());
    return jdbc.query(
        """
        SELECT w.id,w.completed_at,p.name,
          EXISTS(SELECT 1 FROM workout_media m WHERE m.session_id=w.id) AS has_photo,
          EXISTS(SELECT 1 FROM duo_memories d WHERE d.session_id=w.id AND d.duo_id=?) AS shared
        FROM workout_sessions w LEFT JOIN workout_plans p ON p.id=w.plan_version_id
        WHERE w.owner_id=? AND w.status='COMPLETED' ORDER BY w.completed_at DESC,w.id DESC LIMIT 30
        """,
        (rs, n) ->
            new Workout(
                uuid(rs, "id"),
                rs.getString("name") == null ? "Atividade concluída" : rs.getString("name"),
                time(rs, "completed_at"),
                rs.getBoolean("has_photo"),
                rs.getBoolean("shared")),
        duo,
        user.getId());
  }

  @Transactional
  public Panel publish(User user, MemoryRequest request, String key) {
    UUID duo = requireActive(user, true);
    if (key == null || key.isBlank() || key.length() > 120)
      throw conflict("Informe uma chave de publicação válida.");
    var prior =
        jdbc.queryForList(
            "SELECT session_id FROM duo_memories WHERE duo_id=? AND author_id=? AND"
                + " idempotency_key=?",
            UUID.class,
            duo,
            user.getId(),
            key);
    if (!prior.isEmpty()) {
      if (!prior.get(0).equals(request.sessionId()))
        throw conflict("Chave já utilizada em outra atividade.");
      return panel(user);
    }
    var owned =
        jdbc.query(
            "SELECT completed_at FROM workout_sessions WHERE id=? AND owner_id=? AND"
                + " status='COMPLETED'",
            (rs, n) -> time(rs, "completed_at"),
            request.sessionId(),
            user.getId());
    if (owned.isEmpty()) throw missing();
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM duo_memories WHERE duo_id=? AND session_id=?",
            Long.class,
            duo,
            request.sessionId())
        > 0) return panel(user);
    UUID mediaId = null;
    if (Boolean.TRUE.equals(request.includePhoto())) {
      var source =
          workoutMedia
              .findBySessionId(request.sessionId())
              .orElseThrow(
                  () -> conflict("Adicione uma foto ao treino antes de incluí-la na lembrança."));
      if (!source.getOwner().getId().equals(user.getId())) throw missing();
      mediaId = UUID.randomUUID();
      String storageKey = "duo-" + UUID.randomUUID();
      byte[] bytes = storage.read(source.getStorageKey());
      storage.write(storageKey, bytes, source.getMimeType());
      rollbackDelete(storageKey);
      jdbc.update(
          "INSERT INTO"
              + " duo_private_media(id,duo_id,owner_id,storage_key,mime_type,size_bytes,created_at)"
              + " VALUES(?,?,?,?,?,?,?)",
          mediaId,
          duo,
          user.getId(),
          storageKey,
          source.getMimeType(),
          bytes.length,
          ts(Instant.now()));
    }
    jdbc.update(
        "INSERT INTO"
            + " duo_memories(id,duo_id,author_id,session_id,caption,media_id,idempotency_key,completed_at,created_at)"
            + " VALUES(?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        duo,
        user.getId(),
        request.sessionId(),
        clean(request.caption()),
        mediaId,
        key,
        ts(owned.get(0)),
        ts(Instant.now()));
    return panel(user);
  }

  @Transactional
  public void deleteMemory(User user, UUID id) {
    UUID duo = requireActive(user, true);
    var media =
        jdbc.query(
            "SELECT author_id,media_id FROM duo_memories WHERE duo_id=? AND id=?",
            (rs, n) -> new MemoryOwner(uuid(rs, "author_id"), uuid(rs, "media_id")),
            duo,
            id);
    if (media.isEmpty()) throw missing();
    if (!media.get(0).author().equals(user.getId()))
      throw new AccessDeniedException("Somente o autor pode remover esta lembrança.");
    jdbc.update("DELETE FROM duo_memories WHERE id=?", id);
    if (media.get(0).media() != null) {
      String key =
          jdbc.queryForObject(
              "SELECT storage_key FROM duo_private_media WHERE id=?",
              String.class,
              media.get(0).media());
      jdbc.update("DELETE FROM duo_private_media WHERE id=?", media.get(0).media());
      deleteAfterCommit(key);
    }
  }

  @Transactional
  public Panel react(User user, UUID id, ReactionType type, boolean add) {
    UUID duo = requireActive(user, true);
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM duo_memories WHERE id=? AND duo_id=?", Long.class, id, duo)
        == 0) throw missing();
    boolean enabled =
        Boolean.TRUE.equals(
            jdbc.queryForObject(
                "SELECT encouragement_enabled FROM duo_members WHERE duo_id=? AND active_user_id=?",
                Boolean.class,
                duo,
                user.getId()));
    if (add && !enabled) throw conflict("Ative os incentivos nas preferências para reagir.");
    if (add) {
      if (jdbc.queryForObject(
              "SELECT COUNT(*) FROM duo_reactions WHERE memory_id=? AND user_id=? AND type=?",
              Long.class,
              id,
              user.getId(),
              type.name())
          == 0)
        jdbc.update(
            "INSERT INTO duo_reactions(memory_id,user_id,type,created_at) VALUES(?,?,?,?)",
            id,
            user.getId(),
            type.name(),
            ts(Instant.now()));
    } else
      jdbc.update(
          "DELETE FROM duo_reactions WHERE memory_id=? AND user_id=? AND type=?",
          id,
          user.getId(),
          type.name());
    return panel(user);
  }

  @Transactional
  public Panel settings(User user, SettingsRequest request) {
    UUID duo = requireActive(user, true);
    if (request.remindersEnabled() != null)
      jdbc.update(
          "UPDATE duo_members SET reminders_enabled=? WHERE duo_id=? AND active_user_id=?",
          request.remindersEnabled(),
          duo,
          user.getId());
    if (request.encouragementEnabled() != null)
      jdbc.update(
          "UPDATE duo_members SET encouragement_enabled=? WHERE duo_id=? AND active_user_id=?",
          request.encouragementEnabled(),
          duo,
          user.getId());
    return panel(user);
  }

  @Transactional(readOnly = true)
  public PrivateMedia media(User user, UUID id) {
    var rows =
        jdbc.query(
            """
            SELECT p.storage_key,p.mime_type FROM duo_private_media p
            JOIN duo_members m ON m.duo_id=p.duo_id AND m.active_user_id=?
            JOIN duo_partnerships d ON d.id=p.duo_id AND d.status='ACTIVE' WHERE p.id=?
            """,
            (rs, n) -> new MediaKey(rs.getString(1), rs.getString(2)),
            user.getId(),
            id);
    if (rows.isEmpty()) throw missing();
    return new PrivateMedia(storage.read(rows.get(0).key()), rows.get(0).type());
  }

  @Transactional
  public void deleteFor(User user) {
    leave(user);
    jdbc.update("DELETE FROM motivation_preferences WHERE user_id=?", user.getId());
    jdbc.update("DELETE FROM duo_members WHERE user_id=?", user.getId());
  }

  private List<Memory> memories(UUID duo, UUID viewer, boolean encouragement) {
    var reactions =
        encouragement
            ? jdbc.query(
                """
SELECT r.memory_id,r.type,COUNT(*) AS total,
  SUM(CASE WHEN r.user_id=? THEN 1 ELSE 0 END) AS mine
FROM duo_reactions r JOIN (SELECT id FROM duo_memories WHERE duo_id=? ORDER BY created_at DESC LIMIT 30) m ON m.id=r.memory_id
GROUP BY r.memory_id,r.type
""",
                (rs, n) ->
                    new ReactionRow(
                        uuid(rs, "memory_id"),
                        rs.getString("type"),
                        rs.getLong("total"),
                        rs.getLong("mine") > 0),
                viewer,
                duo)
            : List.<ReactionRow>of();
    return jdbc.query(
        """
        SELECT m.id,m.session_id,m.author_id,u.name,m.completed_at,m.caption,m.media_id
        FROM duo_memories m JOIN users u ON u.id=m.author_id
        WHERE m.duo_id=? ORDER BY m.created_at DESC,m.id DESC LIMIT 30
        """,
        (rs, n) -> {
          UUID id = uuid(rs, "id"), photo = uuid(rs, "media_id");
          var visible =
              encouragement
                  ? Arrays.stream(ReactionType.values())
                      .map(
                          type -> {
                            var r =
                                reactions.stream()
                                    .filter(
                                        x -> x.memory().equals(id) && x.type().equals(type.name()))
                                    .findFirst();
                            return new Reaction(
                                type.name(),
                                r.map(ReactionRow::count).orElse(0L),
                                r.map(ReactionRow::mine).orElse(false));
                          })
                      .toList()
                  : List.<Reaction>of();
          return new Memory(
              id,
              uuid(rs, "session_id"),
              uuid(rs, "author_id"),
              rs.getString("name"),
              time(rs, "completed_at"),
              rs.getString("caption"),
              photo == null ? null : "/api/duo/media/" + photo,
              visible);
        },
        duo);
  }

  private UUID activeId(UUID user) {
    var ids =
        jdbc.queryForList(
            "SELECT m.duo_id FROM duo_members m JOIN duo_partnerships d ON d.id=m.duo_id WHERE"
                + " m.active_user_id=? AND d.status='ACTIVE'",
            UUID.class,
            user);
    return ids.isEmpty() ? null : ids.get(0);
  }

  private UUID requireActive(User user, boolean lock) {
    UUID id = activeId(user.getId());
    if (id == null) throw conflict("Crie ou aceite uma parceria primeiro.");
    if (lock)
      jdbc.queryForObject("SELECT id FROM duo_partnerships WHERE id=? FOR UPDATE", UUID.class, id);
    if (!id.equals(activeId(user.getId()))) throw conflict("Esta parceria foi encerrada.");
    return id;
  }

  private Invitation currentInvite(UUID user) {
    var list =
        jdbc.query(
            "SELECT token,expires_at FROM duo_invites WHERE issuer_id=? AND duo_id IS NULL AND"
                + " expires_at>?",
            (rs, n) -> new Invitation(rs.getString(1), time(rs, "expires_at")),
            user,
            ts(Instant.now()));
    return list.isEmpty() ? null : list.get(0);
  }

  private Invite findInvite(String token, boolean lock) {
    if (token == null || !token.matches("[a-fA-F0-9]{64}")) throw missing();
    var list =
        jdbc.query(
            "SELECT issuer_id,expires_at,duo_id FROM duo_invites WHERE token=?"
                + (lock ? " FOR UPDATE" : ""),
            (rs, n) ->
                new Invite(uuid(rs, "issuer_id"), time(rs, "expires_at"), uuid(rs, "duo_id")),
            token);
    if (list.isEmpty()) throw missing();
    return list.get(0);
  }

  private void lockUsers(UUID... users) {
    Arrays.stream(users)
        .distinct()
        .sorted()
        .forEach(
            id -> {
              var rows =
                  jdbc.queryForList(
                      "SELECT id FROM users WHERE id=? AND active=TRUE FOR UPDATE", UUID.class, id);
              if (rows.isEmpty()) throw missing();
            });
  }

  private List<Person> people(UUID duo, UUID viewer) {
    return jdbc.query(
        "SELECT u.id,u.name FROM duo_members m JOIN users u ON u.id=m.user_id WHERE m.duo_id=? AND"
            + " m.active_user_id IS NOT NULL ORDER BY u.id",
        (rs, n) -> new Person(uuid(rs, "id"), rs.getString("name")),
        duo);
  }

  private List<Day> activeDays(List<UUID> users, LocalDate from, LocalDate until) {
    if (users.size() != 2) return List.of();
    return jdbc.query(
        """
SELECT owner_id,CAST(completed_at AT TIME ZONE 'America/Sao_Paulo' AS DATE) AS active_day
FROM workout_sessions WHERE owner_id IN (?,?) AND status='COMPLETED' AND completed_at>=? AND completed_at<?
GROUP BY owner_id,CAST(completed_at AT TIME ZONE 'America/Sao_Paulo' AS DATE)
""",
        (rs, n) -> new Day(uuid(rs, "owner_id"), rs.getObject("active_day", LocalDate.class)),
        users.get(0),
        users.get(1),
        ts(from.atStartOfDay(ZONE).toInstant()),
        ts(until.atStartOfDay(ZONE).toInstant()));
  }

  private List<Goal> goals(List<UUID> users) {
    if (users.size() != 2) return List.of();
    return jdbc.query(
        "SELECT user_id,target,valid_from,valid_until FROM weekly_goal_versions WHERE user_id IN"
            + " (?,?) AND (valid_until IS NULL OR valid_until>=?) ORDER BY valid_from DESC",
        (rs, n) ->
            new Goal(
                uuid(rs, "user_id"),
                rs.getInt("target"),
                time(rs, "valid_from"),
                time(rs, "valid_until")),
        users.get(0),
        users.get(1),
        ts(LocalDate.now(ZONE).withDayOfMonth(1).minusWeeks(1).atStartOfDay(ZONE).toInstant()));
  }

  private static int target(List<Goal> goals, UUID user, Instant at) {
    return goals.stream()
        .filter(
            g ->
                g.user().equals(user)
                    && !g.from().isAfter(at)
                    && (g.until() == null || g.until().isAfter(at)))
        .findFirst()
        .map(Goal::target)
        .orElse(3);
  }

  private static long count(List<Day> days, UUID id, LocalDate from, LocalDate until) {
    return days.stream()
        .filter(d -> d.user().equals(id) && !d.date().isBefore(from) && d.date().isBefore(until))
        .count();
  }

  private static LocalDate week(LocalDate day) {
    return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }

  private static Timestamp ts(Instant value) {
    return Timestamp.from(value);
  }

  private static UUID uuid(ResultSet rs, String field) throws SQLException {
    return rs.getObject(field, UUID.class);
  }

  private static Instant time(ResultSet rs, String field) throws SQLException {
    var value = rs.getObject(field, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private static String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static void validateFuture(Instant value) {
    if (value == null
        || !value.isAfter(Instant.now())
        || value.isAfter(Instant.now().plus(Duration.ofDays(366))))
      throw conflict("Escolha um horário futuro, dentro do próximo ano.");
  }

  private static DomainConflictException conflict(String message) {
    return new DomainConflictException(message);
  }

  private static ResourceNotFoundException missing() {
    return new ResourceNotFoundException("Recurso privado não encontrado.");
  }

  private void deleteAfterCommit(String key) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              storage.delete(key);
            } catch (RuntimeException ignored) {
              /* Reconciliation can remove an inaccessible orphan. */
            }
          }
        });
  }

  private void rollbackDelete(String key) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED)
              try {
                storage.delete(key);
              } catch (RuntimeException ignored) {
              }
          }
        });
  }

  private record Invite(UUID issuer, Instant expires, UUID duo) {}

  private record Person(UUID id, String name) {}

  private record Goal(UUID user, int target, Instant from, Instant until) {}

  private record Day(UUID user, LocalDate date) {}

  private record MeetingState(UUID proposer, String status, Instant scheduled) {}

  private record MemoryOwner(UUID author, UUID media) {}

  private record MediaKey(String key, String type) {}

  private record ReactionRow(UUID memory, String type, long count, boolean mine) {}

  public record PrivateMedia(byte[] bytes, String mimeType) {}
}
