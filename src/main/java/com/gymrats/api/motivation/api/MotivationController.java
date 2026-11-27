package com.gymrats.api.motivation.api;

import com.gymrats.api.motivation.application.MotivationService;
import com.gymrats.api.user.application.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/motivation")
public class MotivationController {
  private final MotivationService service;
  private final CurrentUserService currentUser;

  public MotivationController(MotivationService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping
  public MotivationService.Response get(Authentication a) {
    return service.response(currentUser.require(a));
  }

  public record Selection(
      @Size(max = 80) String titleId,
      @Size(max = 40) String frameId,
      @Size(max = 40) String itemId) {}

  @PatchMapping("/rewards/selection")
  public MotivationService.Response select(@Valid @RequestBody Selection r, Authentication a) {
    return service.select(currentUser.require(a), r);
  }
}
