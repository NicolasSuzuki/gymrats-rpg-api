package com.gymrats.api.ranking.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ranking_snapshots")
public class RankingSnapshot extends AuditableEntity {
  @Id private UUID id;
  @Column(name = "snapshot_key", nullable = false, length = 180) private String snapshotKey;
  @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
  @Column(nullable = false, length = 16) private String scope;
  @Column(name = "group_id") private UUID groupId;
  @Column(nullable = false, length = 16) private String period;
  @Column(nullable = false, length = 20) private String metric;
  @Column(name = "period_start", nullable = false) private Instant periodStart;
  @Column(name = "period_end", nullable = false) private Instant periodEnd;
  @Column(nullable = false) private int position;
  @Column(name = "score_value", nullable = false) private int value;
  @Column(name = "distinct_days", nullable = false) private int distinctDays;
  @Column(name = "check_ins", nullable = false) private int checkIns;

  protected RankingSnapshot() {}
  public RankingSnapshot(
      String snapshotKey,
      User user,
      String scope,
      UUID groupId,
      String period,
      String metric,
      Instant periodStart,
      Instant periodEnd,
      int position,
      int value,
      int distinctDays,
      int checkIns) {
    this.id = UUID.randomUUID();
    this.snapshotKey = snapshotKey;
    this.user = user;
    this.scope = scope;
    this.groupId = groupId;
    this.period = period;
    this.metric = metric;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
    this.position = position;
    this.value = value;
    this.distinctDays = distinctDays;
    this.checkIns = checkIns;
  }
  public void refresh(int position, int value, int distinctDays, int checkIns) {
    this.position = position;
    this.value = value;
    this.distinctDays = distinctDays;
    this.checkIns = checkIns;
  }
}
