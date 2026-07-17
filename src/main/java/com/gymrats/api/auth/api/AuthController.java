package com.gymrats.api.auth.api;

import com.gymrats.api.auth.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;
  public AuthController(AuthService authService) { this.authService = authService; }
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "Cria uma conta e retorna um token")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return authService.register(request); }
  @PostMapping("/login") @Operation(summary = "Autentica com e-mail e senha")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }
}
