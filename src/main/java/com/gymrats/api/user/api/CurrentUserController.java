package com.gymrats.api.user.api;

import com.gymrats.api.user.domain.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class CurrentUserController {
  private final UserRepository users;
  public CurrentUserController(UserRepository users) { this.users = users; }
  @GetMapping("/me") @Operation(summary = "Retorna o usuário autenticado") @SecurityRequirement(name = "bearerAuth")
  public UserResponse me(Authentication authentication) { return users.findByEmailIgnoreCase(authentication.getName()).map(UserResponse::from).orElseThrow(); }
}
