package com.gymrats.api.progression.api;

import com.gymrats.api.progression.application.ProgressionService;
import com.gymrats.api.progression.application.AdvancedProgressionService;
import com.gymrats.api.progression.application.MonarchService;
import com.gymrats.api.user.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class ProgressionController {
  private final ProgressionService progression;
  private final CurrentUserService currentUser;
  private final AdvancedProgressionService advancedProgression;
  private final MonarchService monarchs;

  public ProgressionController(
      ProgressionService progression,
      CurrentUserService currentUser,
      AdvancedProgressionService advancedProgression,
      MonarchService monarchs) {
    this.progression = progression;
    this.currentUser = currentUser;
    this.advancedProgression = advancedProgression;
    this.monarchs = monarchs;
  }

  @GetMapping("/progression")
  @Operation(summary = "Retorna XP, nível, rank e constância da pessoa autenticada")
  ProgressionOverviewResponse overview(Authentication authentication) {
    return progression.overview(currentUser.require(authentication));
  }

  @GetMapping("/progression/history")
  @Operation(summary = "Retorna o ledger imutável de progressão")
  java.util.List<ProgressionEventResponse> history(Authentication authentication) {
    return progression.history(currentUser.require(authentication));
  }

  @GetMapping("/progression/achievements")
  @Operation(summary = "Lista conquistas bloqueadas e desbloqueadas")
  java.util.List<AchievementResponse> achievements(Authentication authentication) {
    return progression.achievements(currentUser.require(authentication));
  }

  @GetMapping("/progression/advanced")
  @Operation(summary = "Retorna maestria por classe e progressão S/Nacional")
  AdvancedProgressionResponse advanced(Authentication authentication) {
    return advancedProgression.overview(currentUser.require(authentication));
  }

  @GetMapping("/progression/hall-of-fame")
  @Operation(summary = "Retorna o Monarca atual e o histórico de reinados")
  HallOfFameResponse hallOfFame(Authentication authentication) {
    return monarchs.hallOfFame(currentUser.require(authentication));
  }

  @PatchMapping("/progression/advanced/pause")
  @Operation(summary = "Congela ou retoma a avaliação de progressão avançada")
  AdvancedProgressionResponse pause(
      @Valid @RequestBody UpdateProgressionPauseRequest request,
      Authentication authentication) {
    return advancedProgression.updatePause(
        currentUser.require(authentication), request.paused(), request.reason());
  }

  @PatchMapping("/profile/title")
  @Operation(summary = "Seleciona um título já desbloqueado")
  Map<String, String> title(
      @Valid @RequestBody SelectTitleRequest request, Authentication authentication) {
    return Map.of(
        "title", progression.selectTitle(currentUser.require(authentication), request.title()));
  }
}
