package com.gymrats.api.social.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class SocialModels {
  private SocialModels() {}

  public record UserResponse(UUID id, String displayName, String title, String friendshipStatus) {}
  public record FriendRequestResponse(
      UUID id,
      UserResponse user,
      String direction,
      String status,
      Instant createdAt) {}
  public record SendFriendRequest(@NotNull UUID userId) {}
  public record DecideFriendRequest(@NotBlank String status) {}
  public record GroupRequest(
      @NotBlank @Size(max = 80) String name,
      @Size(max = 300) String description,
      @NotBlank String visibility) {}
  public record GroupResponse(
      UUID id,
      String name,
      String description,
      String visibility,
      UUID adminId,
      String role,
      long memberCount) {}
  public record MemberResponse(UUID id, String displayName, String role) {}
  public record TransferAdminRequest(@NotNull UUID userId) {}
  public record InviteResponse(
      String token, GroupResponse group, Instant expiresAt, boolean available) {}
}
