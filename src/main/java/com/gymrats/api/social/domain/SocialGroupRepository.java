package com.gymrats.api.social.domain;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialGroupRepository extends JpaRepository<SocialGroup, UUID> {
  List<SocialGroup> findByAdminId(UUID adminId);
}
