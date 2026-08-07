package com.gymrats.api.notification.api;

import static com.gymrats.api.notification.api.NotificationModels.*;

import com.gymrats.api.notification.application.NotificationService;
import com.gymrats.api.user.application.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
  private final NotificationService service;
  private final CurrentUserService currentUser;
  public NotificationController(NotificationService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }
  @GetMapping
  Page list(@RequestParam(defaultValue = "1") int page, Authentication auth) {
    return service.list(currentUser.require(auth), page);
  }
  @PatchMapping
  ReadResponse readAll(Authentication auth) {
    return service.readAll(currentUser.require(auth));
  }
  @PatchMapping("/{id}")
  ReadResponse read(@PathVariable UUID id, Authentication auth) {
    return service.read(currentUser.require(auth), id);
  }
  @GetMapping("/preferences")
  Preferences preferences(Authentication auth) {
    return service.preferences(currentUser.require(auth));
  }
  @PatchMapping("/preferences")
  Preferences preferences(@RequestBody Preferences request, Authentication auth) {
    return service.preferences(currentUser.require(auth), request);
  }
}
