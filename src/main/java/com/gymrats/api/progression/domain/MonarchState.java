package com.gymrats.api.progression.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "monarch_state")
public class MonarchState {
  @Id
  @Column(name = "singleton_id")
  private Short singletonId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_reign_id", unique = true)
  private MonarchReign currentReign;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected MonarchState() {}

  public MonarchReign getCurrentReign() {
    return currentReign;
  }

  public void assign(MonarchReign reign, Instant now) {
    this.currentReign = reign;
    this.updatedAt = now;
  }
}
