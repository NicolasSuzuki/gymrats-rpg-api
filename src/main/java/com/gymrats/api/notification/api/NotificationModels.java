package com.gymrats.api.notification.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NotificationModels {
  private NotificationModels() {}
  public record Item(
      UUID id,
      String eventId,
      String kind,
      String title,
      String message,
      Instant createdAt,
      Instant readAt,
      String href) {}
  public record Page(List<Item> items, int page, int totalPages, long unreadCount) {}
  public record ReadResponse(boolean read, long unreadCount) {}
  public record Preferences(Map<String, Boolean> enabled) {}
}
