package com.gymrats.api.feed.api;

import com.gymrats.api.feed.application.FeedService;
import com.gymrats.api.feed.domain.FeedPostCategory;
import com.gymrats.api.user.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class FeedController {
  private final FeedService service;
  private final CurrentUserService currentUser;

  public FeedController(FeedService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @PostMapping("/workout-sessions/{id}/publish")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Publica uma única vez um treino concluído")
  public FeedPostResponse publish(
      @PathVariable UUID id,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody PublishWorkoutRequest request,
      Authentication authentication) {
    return service.publish(currentUser.require(authentication), id, idempotencyKey, request);
  }

  @GetMapping("/feed")
  public FeedPageResponse feed(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) FeedPostCategory category,
      @RequestParam(defaultValue = "5") int pageSize) {
    return service.list(cursor, category, pageSize);
  }

  @GetMapping("/feed/{id}/comments")
  public List<FeedCommentResponse> comments(
      @PathVariable UUID id, Authentication authentication) {
    return service.comments(currentUser.require(authentication), id);
  }

  @PostMapping("/feed/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public FeedCommentResponse comment(
      @PathVariable UUID id,
      @Valid @RequestBody CreateCommentRequest request,
      Authentication authentication) {
    return service.comment(currentUser.require(authentication), id, request);
  }

  @DeleteMapping("/feed/{postId}/comments/{commentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteComment(
      @PathVariable UUID postId,
      @PathVariable UUID commentId,
      Authentication authentication) {
    service.deleteComment(currentUser.require(authentication), postId, commentId);
  }

  @GetMapping("/profile/workout-diary")
  public List<FeedPostResponse> diary(Authentication authentication) {
    return service.diary(currentUser.require(authentication));
  }
}
