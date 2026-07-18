package com.gymrats.api.auth.application;

import com.gymrats.api.auth.api.*;
import com.gymrats.api.security.JwtService;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.UserRepository;
import com.gymrats.api.user.api.UserResponse;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  public AuthService(UserRepository users, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) { this.users = users; this.passwordEncoder = passwordEncoder; this.authenticationManager = authenticationManager; this.jwtService = jwtService; }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    var email = normalize(request.email());
    if (users.existsByEmailIgnoreCase(email)) throw new EmailAlreadyUsedException();
    var user = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
    return tokenFor(user);
  }

  public AuthResponse login(LoginRequest request) {
    var email = normalize(request.email());
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
    return tokenFor(users.findByEmailIgnoreCase(email).orElseThrow());
  }

  @Transactional(readOnly = true)
  public UserResponse currentUser(String email) {
    return users.findByEmailIgnoreCase(email).map(UserResponse::from).orElseThrow();
  }

  private AuthResponse tokenFor(User user) { return new AuthResponse(jwtService.generate(user), "Bearer", jwtService.expirationSeconds()); }
  private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
