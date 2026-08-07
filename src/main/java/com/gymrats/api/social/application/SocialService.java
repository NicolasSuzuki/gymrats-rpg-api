package com.gymrats.api.social.application;

import static com.gymrats.api.social.api.SocialModels.*;

import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.shared.application.ResourceNotFoundException;
import com.gymrats.api.notification.application.NotificationService;
import com.gymrats.api.notification.domain.Notification;
import com.gymrats.api.social.domain.*;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialService {
  public static final int GROUP_LIMIT = 20;
  private final UserRepository users;
  private final FriendRequestRepository requests;
  private final FriendshipRepository friendships;
  private final SocialGroupRepository groups;
  private final GroupMemberRepository members;
  private final GroupInviteRepository invites;
  private final NotificationService notifications;

  public SocialService(
      UserRepository users,
      FriendRequestRepository requests,
      FriendshipRepository friendships,
      SocialGroupRepository groups,
      GroupMemberRepository members,
      GroupInviteRepository invites,
      NotificationService notifications) {
    this.users = users;
    this.requests = requests;
    this.friendships = friendships;
    this.groups = groups;
    this.members = members;
    this.invites = invites;
    this.notifications = notifications;
  }

  @Transactional(readOnly = true)
  public List<UserResponse> search(User user, String query) {
    String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    if (needle.length() < 2) return List.of();
    return users.findAll().stream()
        .filter(candidate -> candidate.isActive() && !candidate.getId().equals(user.getId()))
        .filter(candidate -> candidate.getName().toLowerCase(Locale.ROOT).contains(needle))
        .limit(20)
        .map(candidate -> user(candidate, relation(user, candidate)))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<UserResponse> friends(User user) {
    return friendships.findByUserLowIdOrUserHighId(user.getId(), user.getId()).stream()
        .map(value -> user(value.other(user.getId()), "FRIENDS"))
        .sorted(Comparator.comparing(UserResponse::displayName))
        .toList();
  }

  @Transactional
  public FriendRequestResponse request(User user, UUID targetId) {
    var target = requireUser(targetId);
    if (user.getId().equals(targetId)) {
      throw new DomainConflictException("Você não pode enviar uma solicitação para si.");
    }
    if (friendship(user, target).isPresent()) {
      throw new DomainConflictException("Vocês já são amigos.");
    }
    var inverse =
        requests.findFirstBySenderIdAndReceiverIdAndStatus(
            targetId, user.getId(), FriendRequest.Status.PENDING);
    if (inverse.isPresent()) return decide(user, inverse.get().getId(), "ACCEPTED");
    var existing =
        requests.findFirstBySenderIdAndReceiverIdAndStatus(
            user.getId(), targetId, FriendRequest.Status.PENDING);
    var saved = existing.orElseGet(() -> requests.save(new FriendRequest(user, target)));
    notifications.emit(
        target,
        "friend-request:" + saved.getId(),
        Notification.Kind.FRIENDSHIP,
        "Nova solicitação de amizade",
        user.getName() + " quer adicionar você.",
        "/social");
    return request(saved, user);
  }

  @Transactional(readOnly = true)
  public List<FriendRequestResponse> requests(User user) {
    return requests
        .findByReceiverIdAndStatusOrderByCreatedAtDesc(
            user.getId(), FriendRequest.Status.PENDING)
        .stream()
        .map(value -> request(value, user))
        .toList();
  }

  @Transactional
  public FriendRequestResponse decide(User user, UUID requestId, String decision) {
    var request = requests.findById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada."));
    if (!request.getReceiver().getId().equals(user.getId())) {
      throw new AccessDeniedException("Somente o destinatário pode responder.");
    }
    if (request.getStatus() != FriendRequest.Status.PENDING) return request(request, user);
    FriendRequest.Status status;
    try {
      status = FriendRequest.Status.valueOf(decision.toUpperCase(Locale.ROOT));
    } catch (RuntimeException exception) {
      throw new DomainConflictException("Use ACCEPTED ou DECLINED.");
    }
    if (status == FriendRequest.Status.PENDING) {
      throw new DomainConflictException("Use ACCEPTED ou DECLINED.");
    }
    request.decide(status);
    if (status == FriendRequest.Status.ACCEPTED
        && friendship(request.getSender(), request.getReceiver()).isEmpty()) {
      friendships.save(new Friendship(request.getSender(), request.getReceiver()));
      notifications.emit(
          request.getSender(),
          "friend-accepted:" + request.getId(),
          Notification.Kind.FRIENDSHIP,
          "Solicitação aceita",
          user.getName() + " agora faz parte dos seus amigos.",
          "/social");
    }
    return request(requests.save(request), user);
  }

  @Transactional
  public void removeFriend(User user, UUID friendId) {
    var friend = requireUser(friendId);
    var relation = friendship(user, friend)
        .orElseThrow(() -> new ResourceNotFoundException("Amizade não encontrada."));
    friendships.delete(relation);
  }

  @Transactional(readOnly = true)
  public List<GroupResponse> groups(User user) {
    Set<UUID> joined =
        members.findByUserId(user.getId()).stream()
            .map(value -> value.getGroup().getId())
            .collect(java.util.stream.Collectors.toSet());
    return groups.findAll().stream()
        .filter(group ->
            group.getVisibility() == SocialGroup.Visibility.PUBLIC || joined.contains(group.getId()))
        .map(group -> group(group, user))
        .toList();
  }

  @Transactional
  public GroupResponse createGroup(User user, GroupRequest request) {
    var group = groups.save(new SocialGroup(
        request.name(), request.description(), visibility(request.visibility()), user));
    members.save(new GroupMember(group, user, GroupMember.Role.ADMIN));
    return group(group, user);
  }

  @Transactional
  public GroupResponse updateGroup(User user, UUID groupId, GroupRequest request) {
    var group = requireGroup(groupId);
    requireAdmin(user, group);
    group.update(request.name(), request.description(), visibility(request.visibility()));
    return group(groups.save(group), user);
  }

  @Transactional
  public GroupResponse joinPublic(User user, UUID groupId) {
    var group = requireGroup(groupId);
    if (group.getVisibility() != SocialGroup.Visibility.PUBLIC) {
      throw new AccessDeniedException("Grupos privados exigem convite.");
    }
    join(user, group);
    notifyAdminOfJoin(user, group);
    return group(group, user);
  }

  @Transactional(readOnly = true)
  public List<MemberResponse> members(User user, UUID groupId) {
    var group = requireGroup(groupId);
    if (group.getVisibility() == SocialGroup.Visibility.PRIVATE
        && !members.existsByGroupIdAndUserId(groupId, user.getId())) {
      throw new AccessDeniedException("Entre no grupo para consultar os membros.");
    }
    return members.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
        .map(value ->
            new MemberResponse(
                value.getUser().getId(), value.getUser().getName(), value.getRole().name()))
        .toList();
  }

  @Transactional
  public void leave(User user, UUID groupId) {
    var group = requireGroup(groupId);
    var membership = members.findByGroupIdAndUserId(groupId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Participação não encontrada."));
    if (group.getAdmin().getId().equals(user.getId())) {
      throw new DomainConflictException(
          "Transfira a administração antes de sair do grupo.");
    }
    members.delete(membership);
  }

  @Transactional
  public GroupResponse transfer(User user, UUID groupId, UUID targetId) {
    var group = requireGroup(groupId);
    requireAdmin(user, group);
    var targetMember = members.findByGroupIdAndUserId(groupId, targetId)
        .orElseThrow(() -> new DomainConflictException("O novo administrador deve ser membro."));
    var currentMember = members.findByGroupIdAndUserId(groupId, user.getId()).orElseThrow();
    currentMember.changeRole(GroupMember.Role.MEMBER);
    targetMember.changeRole(GroupMember.Role.ADMIN);
    group.transferTo(targetMember.getUser());
    members.save(currentMember);
    members.save(targetMember);
    return group(groups.save(group), user);
  }

  @Transactional
  public InviteResponse createInvite(User user, UUID groupId) {
    var group = requireGroup(groupId);
    requireAdmin(user, group);
    var invite = invites.save(new GroupInvite(
        group,
        user,
        UUID.randomUUID().toString().replace("-", ""),
        Instant.now().plus(7, ChronoUnit.DAYS)));
    return invite(invite, user);
  }

  @Transactional(readOnly = true)
  public InviteResponse invite(User user, String token) {
    return invite(requireInvite(token), user);
  }

  @Transactional
  public GroupResponse joinInvite(User user, String token) {
    var invite = requireInvite(token);
    if (!invite.isAvailable()) {
      throw new DomainConflictException("O convite expirou ou já foi utilizado.");
    }
    join(user, invite.getGroup());
    invite.use(user);
    invites.save(invite);
    notifyAdminOfJoin(user, invite.getGroup());
    return group(invite.getGroup(), user);
  }

  private void join(User user, SocialGroup group) {
    if (members.existsByGroupIdAndUserId(group.getId(), user.getId())) return;
    if (members.countByGroupId(group.getId()) >= GROUP_LIMIT) {
      throw new DomainConflictException("O grupo atingiu o limite de 20 membros.");
    }
    members.save(new GroupMember(group, user, GroupMember.Role.MEMBER));
  }

  private void notifyAdminOfJoin(User user, SocialGroup group) {
    if (!group.getAdmin().getId().equals(user.getId())) {
      notifications.emit(
          group.getAdmin(),
          "group-join:" + group.getId() + ":" + user.getId(),
          Notification.Kind.GROUP,
          "Novo membro no grupo",
          user.getName() + " entrou em " + group.getName() + ".",
          "/social");
    }
  }

  private String relation(User current, User candidate) {
    if (friendship(current, candidate).isPresent()) return "FRIENDS";
    if (requests.findFirstBySenderIdAndReceiverIdAndStatus(
        current.getId(), candidate.getId(), FriendRequest.Status.PENDING).isPresent()) {
      return "PENDING_SENT";
    }
    if (requests.findFirstBySenderIdAndReceiverIdAndStatus(
        candidate.getId(), current.getId(), FriendRequest.Status.PENDING).isPresent()) {
      return "PENDING_RECEIVED";
    }
    return "NONE";
  }

  private Optional<Friendship> friendship(User first, User second) {
    UUID low = first.getId().compareTo(second.getId()) < 0 ? first.getId() : second.getId();
    UUID high = low.equals(first.getId()) ? second.getId() : first.getId();
    return friendships.findByUserLowIdAndUserHighId(low, high);
  }

  private User requireUser(UUID id) {
    return users.findById(id)
        .filter(User::isActive)
        .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
  }

  private SocialGroup requireGroup(UUID id) {
    return groups.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado."));
  }

  private GroupInvite requireInvite(String token) {
    return invites.findByToken(token)
        .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado."));
  }

  private void requireAdmin(User user, SocialGroup group) {
    if (!group.getAdmin().getId().equals(user.getId())) {
      throw new AccessDeniedException("Somente a administração pode alterar o grupo.");
    }
  }

  private SocialGroup.Visibility visibility(String value) {
    try {
      return SocialGroup.Visibility.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (RuntimeException exception) {
      throw new DomainConflictException("Use PUBLIC ou PRIVATE.");
    }
  }

  private UserResponse user(User user, String status) {
    return new UserResponse(user.getId(), user.getName(), user.getSelectedTitle(), status);
  }

  private FriendRequestResponse request(FriendRequest value, User current) {
    boolean sent = value.getSender().getId().equals(current.getId());
    User other = sent ? value.getReceiver() : value.getSender();
    String friendshipStatus =
        switch (value.getStatus()) {
          case ACCEPTED -> "FRIENDS";
          case DECLINED -> "NONE";
          case PENDING -> sent ? "PENDING_SENT" : "PENDING_RECEIVED";
        };
    return new FriendRequestResponse(
        value.getId(),
        user(other, friendshipStatus),
        sent ? "SENT" : "RECEIVED",
        value.getStatus().name(),
        value.getCreatedAt());
  }

  private GroupResponse group(SocialGroup group, User user) {
    String role =
        members.findByGroupIdAndUserId(group.getId(), user.getId())
            .map(value -> value.getRole().name())
            .orElse(null);
    return new GroupResponse(
        group.getId(),
        group.getName(),
        group.getDescription(),
        group.getVisibility().name(),
        group.getAdmin().getId(),
        role,
        members.countByGroupId(group.getId()));
  }

  private InviteResponse invite(GroupInvite invite, User user) {
    return new InviteResponse(
        invite.getToken(),
        group(invite.getGroup(), user),
        invite.getExpiresAt(),
        invite.isAvailable());
  }
}
