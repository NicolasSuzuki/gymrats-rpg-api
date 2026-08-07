package com.gymrats.api.ranking.api;

import com.gymrats.api.ranking.application.RankingService;
import com.gymrats.api.user.application.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rankings")
@SecurityRequirement(name = "bearerAuth")
public class RankingController {
  private final RankingService service;
  private final CurrentUserService currentUser;
  public RankingController(RankingService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }
  @GetMapping
  RankingModels.Result ranking(
      @RequestParam(defaultValue = "WEEKLY") String period,
      @RequestParam(defaultValue = "XP") String metric,
      @RequestParam(required = false) UUID groupId,
      @RequestParam(defaultValue = "1") int page,
      Authentication auth) {
    return service.ranking(currentUser.require(auth), period, metric, groupId, page);
  }
}
