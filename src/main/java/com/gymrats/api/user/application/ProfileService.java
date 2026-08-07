package com.gymrats.api.user.application;

import com.gymrats.api.user.api.OnboardingRequest;
import com.gymrats.api.user.api.UpdateProfileRequest;
import com.gymrats.api.user.api.UserResponse;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.UserRepository;
import com.gymrats.api.user.domain.WeeklyGoalVersion;
import com.gymrats.api.user.domain.WeeklyGoalVersionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
  private final UserRepository users;
  private final WeeklyGoalVersionRepository goals;

  public ProfileService(UserRepository users, WeeklyGoalVersionRepository goals) {
    this.users = users;
    this.goals = goals;
  }

  @Transactional(readOnly = true)
  public UserResponse response(User user) {
    return UserResponse.from(user, currentGoal(user));
  }

  @Transactional
  public UserResponse update(User user, UpdateProfileRequest request) {
    user.updateProfile(request.name(), request.modalities(), request.characterClass(), request.universe());
    if (request.weeklyGoal() != null) updateGoal(user, request.weeklyGoal());
    return UserResponse.from(users.save(user), currentGoal(user));
  }

  @Transactional
  public UserResponse completeOnboarding(User user, OnboardingRequest request) {
    user.completeOnboarding(request.modalities(), request.characterClass());
    updateGoal(user, request.weeklyGoal());
    return UserResponse.from(users.save(user), currentGoal(user));
  }

  private Integer currentGoal(User user) {
    return goals.findFirstByUserIdAndValidUntilIsNullOrderByValidFromDesc(user.getId())
        .map(WeeklyGoalVersion::getTarget)
        .orElse(null);
  }

  private void updateGoal(User user, int target) {
    var current = goals.findFirstByUserIdAndValidUntilIsNullOrderByValidFromDesc(user.getId());
    if (current.isPresent() && current.get().getTarget() == target) return;
    var now = Instant.now();
    current.ifPresent(goal -> {
      goal.endAt(now);
      goals.saveAndFlush(goal);
    });
    goals.save(new WeeklyGoalVersion(user, target, now));
  }
}
