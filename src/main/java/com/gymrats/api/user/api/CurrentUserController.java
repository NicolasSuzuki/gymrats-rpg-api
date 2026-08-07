package com.gymrats.api.user.api;

import com.gymrats.api.user.application.AccountService;
import com.gymrats.api.user.application.CurrentUserService;
import com.gymrats.api.user.application.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class CurrentUserController {
  private final CurrentUserService currentUser;
  private final ProfileService profiles;
  private final AccountService accounts;

  public CurrentUserController(
      CurrentUserService currentUser, ProfileService profiles, AccountService accounts) {
    this.currentUser = currentUser;
    this.profiles = profiles;
    this.accounts = accounts;
  }

  @GetMapping("/me")
  @Operation(summary = "Retorna o usuário autenticado")
  public UserResponse me(Authentication authentication) {
    return profiles.response(currentUser.require(authentication));
  }

  @PatchMapping("/me")
  @Operation(summary = "Atualiza o perfil autenticado")
  public UserResponse update(
      @Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
    return profiles.update(currentUser.require(authentication), request);
  }

  @PatchMapping("/me/onboarding")
  @Operation(summary = "Conclui o onboarding do usuário")
  public UserResponse onboarding(
      @Valid @RequestBody OnboardingRequest request, Authentication authentication) {
    return profiles.completeOnboarding(currentUser.require(authentication), request);
  }

  @DeleteMapping("/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Exclui e anonimiza a conta autenticada")
  public void delete(Authentication authentication) {
    accounts.delete(currentUser.require(authentication));
  }
}
