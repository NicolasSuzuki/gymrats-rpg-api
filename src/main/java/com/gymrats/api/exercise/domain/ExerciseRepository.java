package com.gymrats.api.exercise.domain;

import com.gymrats.api.user.domain.Modality;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
  @Query("""
      select e from Exercise e
      where e.archived = false
        and (e.visibility = com.gymrats.api.exercise.domain.ExerciseVisibility.GLOBAL or e.owner.id = :userId)
        and (:name = '' or lower(e.name) like lower(concat('%', :name, '%')))
        and (:modality is null or e.modality = :modality)
        and (:category = '' or lower(e.category) = lower(:category))
        and (:muscleGroup = '' or lower(coalesce(e.muscleGroup, '')) = lower(:muscleGroup))
      order by e.name
      """)
  Page<Exercise> searchVisible(
      @Param("userId") UUID userId,
      @Param("name") String name,
      @Param("modality") Modality modality,
      @Param("category") String category,
      @Param("muscleGroup") String muscleGroup,
      Pageable pageable);

  Optional<Exercise> findByIdAndArchivedFalse(UUID id);

  interface ExerciseName { UUID getId(); String getName(); }

  @Query("""
      select e.id as id, e.name as name from Exercise e
      where e.id in :ids and
        (e.visibility = com.gymrats.api.exercise.domain.ExerciseVisibility.GLOBAL or e.owner.id = :owner)
      """)
  java.util.List<ExerciseName> findVisibleNames(
      @Param("ids") java.util.Collection<UUID> ids, @Param("owner") UUID owner);
}
