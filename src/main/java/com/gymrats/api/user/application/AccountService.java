package com.gymrats.api.user.application;

import com.gymrats.api.feed.application.MediaService;
import com.gymrats.api.feed.domain.FeedCommentRepository;
import com.gymrats.api.feed.domain.FeedPostRepository;
import com.gymrats.api.social.domain.FriendRequestRepository;
import com.gymrats.api.social.domain.FriendshipRepository;
import com.gymrats.api.social.domain.GroupMember;
import com.gymrats.api.social.domain.GroupMemberRepository;
import com.gymrats.api.social.domain.SocialGroupRepository;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.UserRepository;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final UserRepository users;
  private final MediaService media;
  private final FeedPostRepository posts;
  private final FeedCommentRepository comments;
  private final FriendshipRepository friendships;
  private final FriendRequestRepository requests;
  private final SocialGroupRepository groups;
  private final GroupMemberRepository members;
  private final com.gymrats.api.feedback.application.FeedbackService feedback;

  public AccountService(
      UserRepository users,
      MediaService media,
      FeedPostRepository posts,
      FeedCommentRepository comments,
      FriendshipRepository friendships,
      FriendRequestRepository requests,
      SocialGroupRepository groups,
      GroupMemberRepository members,
      com.gymrats.api.feedback.application.FeedbackService feedback) {
    this.users = users;
    this.media = media;
    this.posts = posts;
    this.comments = comments;
    this.friendships = friendships;
    this.requests = requests;
    this.groups = groups;
    this.members = members;
    this.feedback = feedback;
  }

  @Transactional
  public void delete(User user) {
    feedback.removeFor(user);
    media.removeAllFor(user);
    comments.deleteAll(comments.findByOwnerId(user.getId()));
    posts.deleteAll(posts.findByOwnerId(user.getId()));
    requests.deleteAll(requests.findBySenderIdOrReceiverId(user.getId(), user.getId()));
    friendships.deleteAll(
        friendships.findByUserLowIdOrUserHighId(user.getId(), user.getId()));

    for (var group : new ArrayList<>(groups.findByAdminId(user.getId()))) {
      var groupMembers = new ArrayList<>(members.findByGroupIdOrderByJoinedAtAsc(group.getId()));
      var successor = groupMembers.stream()
          .filter(member -> !member.getUser().getId().equals(user.getId()))
          .findFirst();
      if (successor.isEmpty()) {
        groups.delete(group);
      } else {
        successor.get().changeRole(GroupMember.Role.ADMIN);
        group.transferTo(successor.get().getUser());
        members.save(successor.get());
        groups.save(group);
      }
    }

    members.deleteAll(new ArrayList<>(members.findByUserId(user.getId())));
    user.anonymize();
    users.save(user);
  }
}
