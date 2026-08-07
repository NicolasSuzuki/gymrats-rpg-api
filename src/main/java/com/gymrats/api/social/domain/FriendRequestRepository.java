package com.gymrats.api.social.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {
  List<FriendRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(
      UUID receiverId, FriendRequest.Status status);
  Optional<FriendRequest> findFirstBySenderIdAndReceiverIdAndStatus(
      UUID senderId, UUID receiverId, FriendRequest.Status status);
  List<FriendRequest> findBySenderIdOrReceiverId(UUID senderId, UUID receiverId);
}
