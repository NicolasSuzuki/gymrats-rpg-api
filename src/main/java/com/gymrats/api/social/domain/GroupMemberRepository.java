package com.gymrats.api.social.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
  List<GroupMember> findByGroupIdOrderByJoinedAtAsc(UUID groupId);
  List<GroupMember> findByUserId(UUID userId);
  Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);
  boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
  long countByGroupId(UUID groupId);
}
