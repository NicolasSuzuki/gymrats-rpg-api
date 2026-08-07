package com.gymrats.api.feed.application;

import com.gymrats.api.feed.api.CreateCommentRequest;
import com.gymrats.api.feed.api.FeedCommentResponse;
import com.gymrats.api.feed.api.FeedPageResponse;
import com.gymrats.api.feed.api.FeedPostResponse;
import com.gymrats.api.feed.api.PublishWorkoutRequest;
import com.gymrats.api.feed.domain.FeedComment;
import com.gymrats.api.feed.domain.FeedCommentRepository;
import com.gymrats.api.feed.domain.FeedPost;
import com.gymrats.api.feed.domain.FeedPostCategory;
import com.gymrats.api.feed.domain.FeedPostRepository;
import com.gymrats.api.feed.domain.FeedPublishOperation;
import com.gymrats.api.feed.domain.FeedPublishOperationRepository;
import com.gymrats.api.feed.domain.WorkoutMediaRepository;
import com.gymrats.api.notification.application.NotificationService;
import com.gymrats.api.notification.domain.Notification;
import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.shared.application.ResourceNotFoundException;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.workout.domain.WorkoutSessionRepository;
import com.gymrats.api.workout.domain.WorkoutSessionStatus;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {
  private final FeedPostRepository posts;
  private final FeedCommentRepository comments;
  private final FeedPublishOperationRepository operations;
  private final WorkoutMediaRepository media;
  private final WorkoutSessionRepository sessions;
  private final NotificationService notifications;

  public FeedService(
      FeedPostRepository posts,
      FeedCommentRepository comments,
      FeedPublishOperationRepository operations,
      WorkoutMediaRepository media,
      WorkoutSessionRepository sessions,
      NotificationService notifications) {
    this.posts = posts;
    this.comments = comments;
    this.operations = operations;
    this.media = media;
    this.sessions = sessions;
    this.notifications = notifications;
  }

  @Transactional
  public FeedPostResponse publish(
      User user, UUID sessionId, String idempotencyKey, PublishWorkoutRequest request) {
    validateKey(idempotencyKey);
    var replay = operations.findByOwnerIdAndIdempotencyKey(user.getId(), idempotencyKey);
    if (replay.isPresent()) {
      var operation = replay.get();
      if (!operation.getSession().getId().equals(sessionId)) {
        throw new DomainConflictException("Idempotency-Key já utilizada em outra publicação.");
      }
      return response(operation.getPost());
    }

    var session = sessions.findByIdAndOwnerId(sessionId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Treino concluído não encontrado."));
    if (session.getStatus() != WorkoutSessionStatus.COMPLETED) {
      throw new DomainConflictException("Somente treinos concluídos podem ser publicados.");
    }

    var existing = posts.findByWorkoutSessionId(sessionId).orElse(null);
    if (existing != null) {
      operations.save(new FeedPublishOperation(user, idempotencyKey, session, existing));
      return response(existing);
    }

    String title = session.getPlanVersion() == null
        ? "Treino livre"
        : session.getPlanVersion().getName();
    var post = new FeedPost(
        user,
        session,
        media.findBySessionId(sessionId).orElse(null),
        title,
        request.caption());
    post = posts.save(post);
    operations.save(new FeedPublishOperation(user, idempotencyKey, session, post));
    return response(post);
  }

  @Transactional(readOnly = true)
  public FeedPageResponse list(String cursor, FeedPostCategory category, int requestedPageSize) {
    int pageSize = Math.max(1, Math.min(50, requestedPageSize));
    int page = decodeCursor(cursor);
    var pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    var result = category == null ? posts.findAll(pageable) : posts.findAllByCategory(category, pageable);
    var items = result.getContent().stream().map(this::response).toList();
    return new FeedPageResponse(
        items,
        result.hasNext() ? encodeCursor(page + 1) : null,
        List.of(FeedPostCategory.WORKOUT.name()));
  }

  @Transactional(readOnly = true)
  public List<FeedPostResponse> diary(User user) {
    return posts.findByOwnerIdAndCategoryOrderByCreatedAtDesc(
            user.getId(), FeedPostCategory.WORKOUT)
        .stream()
        .map(this::response)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<FeedCommentResponse> comments(User user, UUID postId) {
    requirePost(postId);
    return comments.findByPostIdOrderByCreatedAtAsc(postId).stream()
        .map(comment -> FeedCommentResponse.from(comment, user.getId()))
        .toList();
  }

  @Transactional
  public FeedCommentResponse comment(User user, UUID postId, CreateCommentRequest request) {
    var post = requirePost(postId);
    var saved = comments.save(new FeedComment(post, user, request.content()));
    if (!post.getOwner().getId().equals(user.getId())) {
      notifications.emit(
          post.getOwner(),
          "comment:" + saved.getId(),
          Notification.Kind.COMMENT,
          "Novo comentário",
          user.getName() + " comentou no seu treino.",
          "/feed");
    }
    return FeedCommentResponse.from(saved, user.getId());
  }

  @Transactional
  public void deleteComment(User user, UUID postId, UUID commentId) {
    var comment = comments.findByIdAndPostId(commentId, postId)
        .orElseThrow(() -> new ResourceNotFoundException("Comentário não encontrado."));
    if (!comment.getOwner().getId().equals(user.getId())) {
      throw new AccessDeniedException("Você só pode remover seu próprio comentário.");
    }
    comments.delete(comment);
  }

  private FeedPost requirePost(UUID postId) {
    return posts.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Publicação não encontrada."));
  }

  private FeedPostResponse response(FeedPost post) {
    return FeedPostResponse.from(post, comments.countByPostId(post.getId()));
  }

  private void validateKey(String key) {
    if (key == null || key.isBlank() || key.length() > 120) {
      throw new DomainConflictException(
          "Idempotency-Key é obrigatório e deve ter até 120 caracteres.");
    }
  }

  private String encodeCursor(int page) {
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(Integer.toString(page).getBytes(StandardCharsets.UTF_8));
  }

  private int decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) return 0;
    try {
      int page = Integer.parseInt(
          new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
      if (page < 0) throw new NumberFormatException();
      return page;
    } catch (IllegalArgumentException exception) {
      throw new DomainConflictException("Cursor de paginação inválido.");
    }
  }
}
