package com.gymrats.api.notification.application;

import static com.gymrats.api.notification.api.NotificationModels.*;

import com.gymrats.api.notification.domain.*;
import com.gymrats.api.shared.application.ResourceNotFoundException;
import com.gymrats.api.user.domain.User;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationRepository notifications;
  private final NotificationPreferenceRepository preferences;

  public NotificationService(
      NotificationRepository notifications, NotificationPreferenceRepository preferences) {
    this.notifications = notifications;
    this.preferences = preferences;
  }

  @Transactional
  public void emit(
      User user,
      String eventId,
      Notification.Kind kind,
      String title,
      String message,
      String href) {
    if (!user.isActive()
        || !preferences.findByUserIdAndKind(user.getId(), kind)
            .map(NotificationPreference::isEnabled)
            .orElse(true)
        || notifications.findByUserIdAndEventId(user.getId(), eventId).isPresent()) {
      return;
    }
    notifications.save(new Notification(user, eventId, kind, title, message, href));
  }

  @Transactional(readOnly = true)
  public Page list(User user, int requestedPage) {
    int page = Math.max(1, requestedPage);
    var result =
        notifications.findByUserIdOrderByCreatedAtDesc(
            user.getId(), PageRequest.of(page - 1, 20));
    return new Page(
        result.getContent().stream().map(this::item).toList(),
        page,
        Math.max(1, result.getTotalPages()),
        notifications.countByUserIdAndReadAtIsNull(user.getId()));
  }

  @Transactional
  public ReadResponse read(User user, UUID id) {
    var notification = notifications.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada."));
    notification.markRead();
    notifications.save(notification);
    return new ReadResponse(true, notifications.countByUserIdAndReadAtIsNull(user.getId()));
  }

  @Transactional
  public ReadResponse readAll(User user) {
    var unread = notifications.findByUserIdAndReadAtIsNull(user.getId());
    unread.forEach(Notification::markRead);
    notifications.saveAll(unread);
    return new ReadResponse(true, 0);
  }

  @Transactional(readOnly = true)
  public Preferences preferences(User user) {
    var saved = preferences.findByUserId(user.getId()).stream()
        .collect(java.util.stream.Collectors.toMap(
            value -> value.getKind().name(), NotificationPreference::isEnabled));
    var result = new LinkedHashMap<String, Boolean>();
    for (var kind : Notification.Kind.values()) result.put(kind.name(), saved.getOrDefault(kind.name(), true));
    return new Preferences(result);
  }

  @Transactional
  public Preferences preferences(User user, Preferences request) {
    if (request.enabled() != null) {
      request.enabled().forEach((name, enabled) -> {
        Notification.Kind kind;
        try {
          kind = Notification.Kind.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
          return;
        }
        var value = preferences.findByUserIdAndKind(user.getId(), kind)
            .orElseGet(() -> new NotificationPreference(user, kind, true));
        value.setEnabled(Boolean.TRUE.equals(enabled));
        preferences.save(value);
      });
    }
    return preferences(user);
  }

  private Item item(Notification value) {
    return new Item(
        value.getId(),
        value.getEventId(),
        value.getKind().name(),
        value.getTitle(),
        value.getMessage(),
        value.getCreatedAt(),
        value.getReadAt(),
        value.getHref());
  }
}
