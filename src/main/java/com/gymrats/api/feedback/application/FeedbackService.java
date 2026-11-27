package com.gymrats.api.feedback.application;

import com.gymrats.api.feedback.api.FeedbackController.Request;
import com.gymrats.api.feedback.api.FeedbackController.Receipt;
import com.gymrats.api.user.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {
  private final JdbcTemplate jdbc;
  public FeedbackService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  @Transactional
  public Receipt create(User user, Request request) {
    var id = UUID.randomUUID();
    var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    var path = request.path();
    if (path != null) {
      path = path.split("[?#]", 2)[0];
      if (path.length() > 300) path = path.substring(0, 300);
    }
    jdbc.update("""
        INSERT INTO beta_feedback(id, user_id, category, description, path, request_id, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """, id, user.getId(), request.category().name(), request.description().trim(), path,
        request.requestId(), java.sql.Timestamp.from(now));
    return new Receipt(id, now);
  }

  @Transactional
  public void removeFor(User user) {
    jdbc.update("DELETE FROM beta_feedback WHERE user_id = ?", user.getId());
  }
}
