package com.gymrats.api.duo.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DuoModels {
  private DuoModels() {}

  public record Invitation(String token, Instant expiresAt) {}

  public record InvitePreview(boolean available, String inviterName, Instant expiresAt) {}

  public record Member(
      UUID userId,
      String name,
      boolean isMe,
      int weeklyTarget,
      long weeklyCompleted,
      double ratio) {}

  public record Cooperative(boolean completed, double progress) {}

  public record Meeting(
      UUID id,
      UUID proposerId,
      Instant scheduledAt,
      String location,
      String note,
      String status,
      boolean canRespond,
      boolean canReschedule) {}

  public record MeetingRequest(
      @NotNull Instant scheduledAt,
      @Size(max = 120) String location,
      @Size(max = 280) String note) {}

  public enum MeetingAction {
    ACCEPT,
    DECLINE,
    RESCHEDULE
  }

  public record MeetingChange(
      @NotNull MeetingAction action,
      Instant scheduledAt,
      @Size(max = 120) String location,
      @Size(max = 280) String note,
      Instant expectedScheduledAt) {
    @AssertTrue(message = "Informe o horário do convite que você está respondendo.")
    public boolean isDecisionTimePresent() {
      return action == null || action == MeetingAction.RESCHEDULE || expectedScheduledAt != null;
    }
  }

  public record MemoryRequest(
      @NotNull UUID sessionId, @Size(max = 280) String caption, Boolean includePhoto) {}

  public enum ReactionType {
    PROUD,
    TOGETHER,
    YOU_CAN
  }

  public record ReactionRequest(@NotNull ReactionType type) {}

  public record Reaction(String type, long count, boolean reacted) {}

  public record Memory(
      UUID id,
      UUID sessionId,
      UUID authorId,
      String authorName,
      Instant completedAt,
      String caption,
      String photoUrl,
      List<Reaction> reactions) {}

  public record Settings(boolean remindersEnabled, boolean encouragementEnabled) {}

  public record SettingsRequest(Boolean remindersEnabled, Boolean encouragementEnabled) {}

  public record Reminder(UUID id, String message, Instant scheduledAt) {}

  public record MonthlyMember(UUID userId, String name, long activeDays) {}

  public record MonthlyRecap(
      String month,
      long activeDays,
      long sharedMemories,
      long cooperativeWeeks,
      List<MonthlyMember> members) {}

  public record Workout(
      UUID id, String title, Instant completedAt, boolean hasPhoto, boolean shared) {}

  public record Panel(
      String status,
      UUID id,
      LocalDate weekStart,
      List<Member> members,
      Cooperative cooperative,
      Invitation invitation,
      List<Meeting> meetings,
      List<Memory> memories,
      Settings settings,
      List<Reminder> reminders,
      MonthlyRecap monthlyRecap) {}
}
