package com.gymrats.api.progression.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MonarchStateRepository extends JpaRepository<MonarchState, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select state from MonarchState state where state.singletonId = 1")
  Optional<MonarchState> lockSingleton();
}
