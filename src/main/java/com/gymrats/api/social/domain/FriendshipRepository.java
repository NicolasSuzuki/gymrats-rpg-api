package com.gymrats.api.social.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
  List<Friendship> findByUserLowIdOrUserHighId(UUID lowId, UUID highId);
  Optional<Friendship> findByUserLowIdAndUserHighId(UUID lowId, UUID highId);
}
