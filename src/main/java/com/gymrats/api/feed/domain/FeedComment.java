package com.gymrats.api.feed.domain;

import com.gymrats.api.shared.domain.AuditableEntity;
import com.gymrats.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "feed_comments")
public class FeedComment extends AuditableEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "post_id", nullable = false)
  private FeedPost post;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(nullable = false, length = 500)
  private String content;

  protected FeedComment() {}

  public FeedComment(FeedPost post, User owner, String content) {
    this.id = UUID.randomUUID();
    this.post = post;
    this.owner = owner;
    this.content = content.trim();
  }

  public UUID getId() { return id; }
  public FeedPost getPost() { return post; }
  public User getOwner() { return owner; }
  public String getContent() { return content; }
}
