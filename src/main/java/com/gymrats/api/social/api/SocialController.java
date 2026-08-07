package com.gymrats.api.social.api;

import static com.gymrats.api.social.api.SocialModels.*;

import com.gymrats.api.social.application.SocialService;
import com.gymrats.api.user.application.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social")
@SecurityRequirement(name = "bearerAuth")
public class SocialController {
  private final SocialService service;
  private final CurrentUserService currentUser;

  public SocialController(SocialService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping("/users")
  List<UserResponse> search(@RequestParam(defaultValue = "") String query, Authentication auth) {
    return service.search(currentUser.require(auth), query);
  }

  @GetMapping("/friends")
  List<UserResponse> friends(Authentication auth) {
    return service.friends(currentUser.require(auth));
  }

  @DeleteMapping("/friends/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void remove(@PathVariable UUID id, Authentication auth) {
    service.removeFriend(currentUser.require(auth), id);
  }

  @GetMapping("/friend-requests")
  List<FriendRequestResponse> requests(Authentication auth) {
    return service.requests(currentUser.require(auth));
  }

  @PostMapping("/friend-requests")
  @ResponseStatus(HttpStatus.CREATED)
  FriendRequestResponse request(
      @Valid @RequestBody SendFriendRequest request, Authentication auth) {
    return service.request(currentUser.require(auth), request.userId());
  }

  @PatchMapping("/friend-requests/{id}")
  FriendRequestResponse decide(
      @PathVariable UUID id,
      @Valid @RequestBody DecideFriendRequest request,
      Authentication auth) {
    return service.decide(currentUser.require(auth), id, request.status());
  }

  @GetMapping("/groups")
  List<GroupResponse> groups(Authentication auth) {
    return service.groups(currentUser.require(auth));
  }

  @PostMapping("/groups")
  @ResponseStatus(HttpStatus.CREATED)
  GroupResponse create(@Valid @RequestBody GroupRequest request, Authentication auth) {
    return service.createGroup(currentUser.require(auth), request);
  }

  @PatchMapping("/groups/{id}")
  GroupResponse update(
      @PathVariable UUID id, @Valid @RequestBody GroupRequest request, Authentication auth) {
    return service.updateGroup(currentUser.require(auth), id, request);
  }

  @PostMapping("/groups/{id}/join")
  GroupResponse join(@PathVariable UUID id, Authentication auth) {
    return service.joinPublic(currentUser.require(auth), id);
  }

  @GetMapping("/groups/{id}/members")
  List<MemberResponse> members(@PathVariable UUID id, Authentication auth) {
    return service.members(currentUser.require(auth), id);
  }

  @DeleteMapping("/groups/{id}/membership")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void leave(@PathVariable UUID id, Authentication auth) {
    service.leave(currentUser.require(auth), id);
  }

  @PatchMapping("/groups/{id}/admin")
  GroupResponse transfer(
      @PathVariable UUID id,
      @Valid @RequestBody TransferAdminRequest request,
      Authentication auth) {
    return service.transfer(currentUser.require(auth), id, request.userId());
  }

  @PostMapping("/groups/{id}/invites")
  @ResponseStatus(HttpStatus.CREATED)
  InviteResponse createInvite(@PathVariable UUID id, Authentication auth) {
    return service.createInvite(currentUser.require(auth), id);
  }

  @GetMapping("/group-invites/{token}")
  InviteResponse invite(@PathVariable String token, Authentication auth) {
    return service.invite(currentUser.require(auth), token);
  }

  @PostMapping("/group-invites/{token}/join")
  GroupResponse joinInvite(@PathVariable String token, Authentication auth) {
    return service.joinInvite(currentUser.require(auth), token);
  }
}
