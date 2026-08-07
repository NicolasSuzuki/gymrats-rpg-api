package com.gymrats.api.user.application;

import com.gymrats.api.shared.application.ResourceNotFoundException;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.user.domain.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
  private final UserRepository users;

  public CurrentUserService(UserRepository users) { this.users = users; }

  public User require(Authentication authentication) {
    return users.findByEmailIgnoreCase(authentication.getName())
        .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
  }
}
