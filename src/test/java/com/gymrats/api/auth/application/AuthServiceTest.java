package com.gymrats.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gymrats.api.auth.api.RegisterRequest;
import com.gymrats.api.security.JwtService;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
  private final UserRepository users = mock(UserRepository.class);
  private final PasswordEncoder encoder = mock(PasswordEncoder.class);
  private final AuthenticationManager authentication = mock(AuthenticationManager.class);
  private final JwtService jwt = mock(JwtService.class);
  private AuthService service;
  @BeforeEach void setUp() { reset(users, encoder, authentication, jwt); service = new AuthService(users, encoder, authentication, jwt); }
  @Test void registersUserWithNormalizedEmailAndEncodedPassword() {
    when(encoder.encode("password123")).thenReturn("hash");
    when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwt.generate(any(User.class))).thenReturn("token"); when(jwt.expirationSeconds()).thenReturn(3600L);
    var response = service.register(new RegisterRequest("Nicolas", " NICOLAS@EXAMPLE.COM ", "password123"));
    assertThat(response.accessToken()).isEqualTo("token");
    verify(users).save(argThat(user -> user.getEmail().equals("nicolas@example.com") && user.getPasswordHash().equals("hash")));
  }
  @Test void rejectsDuplicatedEmail() {
    when(users.existsByEmailIgnoreCase("nicolas@example.com")).thenReturn(true);
    assertThatThrownBy(() -> service.register(new RegisterRequest("Nicolas", "nicolas@example.com", "password123"))).isInstanceOf(EmailAlreadyUsedException.class);
    verify(users, never()).save(any());
  }
}
