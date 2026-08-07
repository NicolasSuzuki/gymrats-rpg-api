package com.gymrats.api.social.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, UUID> {
  Optional<GroupInvite> findByToken(String token);
}
