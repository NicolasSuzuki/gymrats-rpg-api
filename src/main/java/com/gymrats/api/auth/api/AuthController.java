package com.gymrats.api.auth.api;

import com.gymrats.api.auth.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.gymrats.api.user.api.UserResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;
  public AuthController(AuthService authService) { this.authService = authService; }
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "Cria uma conta e retorna um token")
  @ApiResponses({@ApiResponse(responseCode = "201", description = "Conta criada"), @ApiResponse(responseCode = "400", description = "Campos inválidos"), @ApiResponse(responseCode = "409", description = "E-mail já utilizado")})
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return authService.register(request); }
  @PostMapping("/login") @Operation(summary = "Autentica com e-mail e senha")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Credenciais válidas"), @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou usuário inativo")})
  public AuthResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }
  @GetMapping("/me") @Operation(summary = "Retorna o usuário autenticado") @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuário autenticado"), @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado")})
  public UserResponse me(Authentication authentication) { return authService.currentUser(authentication.getName()); }
}
