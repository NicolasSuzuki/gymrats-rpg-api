package com.gymrats.api.duo.api;

import com.gymrats.api.duo.api.DuoModels.*;
import com.gymrats.api.duo.application.DuoService;
import com.gymrats.api.user.application.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/duo")
public class DuoController {
  private final DuoService service;
  private final CurrentUserService currentUser;

  public DuoController(DuoService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping
  public Panel panel(Authentication a) {
    return service.panel(currentUser.require(a));
  }

  @PostMapping("/invites")
  @ResponseStatus(HttpStatus.CREATED)
  public Invitation invite(Authentication a) {
    return service.invite(currentUser.require(a));
  }

  @DeleteMapping("/invites")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelInvite(Authentication a) {
    service.cancelInvite(currentUser.require(a));
  }

  @GetMapping("/invites/{token}")
  public InvitePreview preview(@PathVariable String token, Authentication a) {
    return service.preview(currentUser.require(a), token);
  }

  @PostMapping("/invites/{token}/join")
  public Panel join(@PathVariable String token, Authentication a) {
    return service.join(currentUser.require(a), token);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leave(Authentication a) {
    service.leave(currentUser.require(a));
  }

  @PostMapping("/meetings")
  public Panel propose(@Valid @RequestBody MeetingRequest r, Authentication a) {
    return service.propose(currentUser.require(a), r);
  }

  @PatchMapping("/meetings/{id}")
  public Panel meeting(
      @PathVariable UUID id, @Valid @RequestBody MeetingChange r, Authentication a) {
    return service.meeting(currentUser.require(a), id, r);
  }

  @GetMapping("/workouts")
  public List<Workout> workouts(Authentication a) {
    return service.workouts(currentUser.require(a));
  }

  @PostMapping("/memories")
  public Panel publish(
      @Valid @RequestBody MemoryRequest r,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      Authentication a) {
    return service.publish(currentUser.require(a), r, key);
  }

  @DeleteMapping("/memories/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Authentication a) {
    service.deleteMemory(currentUser.require(a), id);
  }

  @PutMapping("/memories/{id}/reaction")
  public Panel react(
      @PathVariable UUID id, @Valid @RequestBody ReactionRequest r, Authentication a) {
    return service.react(currentUser.require(a), id, r.type(), true);
  }

  @DeleteMapping("/memories/{id}/reaction")
  public Panel unreact(@PathVariable UUID id, @RequestParam ReactionType type, Authentication a) {
    return service.react(currentUser.require(a), id, type, false);
  }

  @PatchMapping("/settings")
  public Panel settings(@RequestBody SettingsRequest r, Authentication a) {
    return service.settings(currentUser.require(a), r);
  }

  @GetMapping("/media/{id}")
  public ResponseEntity<ByteArrayResource> media(@PathVariable UUID id, Authentication a) {
    var media = service.media(currentUser.require(a), id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(media.mimeType()))
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=lembranca")
        .body(new ByteArrayResource(media.bytes()));
  }
}
