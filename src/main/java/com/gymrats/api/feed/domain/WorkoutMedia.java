package com.gymrats.api.feed.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.workout.domain.WorkoutSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "workout_media")
public class WorkoutMedia extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false, unique = true)
  private WorkoutSession session;

  @Column(name = "storage_key", nullable = false, unique = true, length = 160)
  private String storageKey;

  @Column(name = "original_file_name", nullable = false, length = 255)
  private String originalFileName;

  @Column(name = "mime_type", nullable = false, length = 40)
  private String mimeType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 64)
  private String sha256;

  protected WorkoutMedia() {}

  public WorkoutMedia(
      User owner,
      WorkoutSession session,
      String storageKey,
      String originalFileName,
      String mimeType,
      long sizeBytes,
      String sha256) {
    this.id = UUID.randomUUID();
    this.owner = owner;
    this.session = session;
    replace(storageKey, originalFileName, mimeType, sizeBytes, sha256);
  }

  public void replace(
      String storageKey,
      String originalFileName,
      String mimeType,
      long sizeBytes,
      String sha256) {
    this.storageKey = storageKey;
    this.originalFileName = originalFileName;
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
    this.sha256 = sha256;
  }

  public UUID getId() { return id; }
  public User getOwner() { return owner; }
  public WorkoutSession getSession() { return session; }
  public String getStorageKey() { return storageKey; }
  public String getOriginalFileName() { return originalFileName; }
  public String getMimeType() { return mimeType; }
  public long getSizeBytes() { return sizeBytes; }
  public String getSha256() { return sha256; }
}
