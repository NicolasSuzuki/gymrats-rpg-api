package com.gymrats.api.feed.api;

import com.gymrats.api.feed.application.MediaService;
import com.gymrats.api.user.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class MediaController {
  private final MediaService service;
  private final CurrentUserService currentUser;

  public MediaController(MediaService service, CurrentUserService currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @PostMapping(
      path = "/workout-sessions/{id}/photo",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Adiciona ou substitui a foto de um treino concluído")
  public WorkoutMediaResponse attach(
      @PathVariable UUID id,
      @RequestPart("photo") MultipartFile photo,
      Authentication authentication) {
    return service.attach(currentUser.require(authentication), id, photo);
  }

  @DeleteMapping("/workout-sessions/{id}/photo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable UUID id, Authentication authentication) {
    service.remove(currentUser.require(authentication), id);
  }

  @GetMapping("/media/{id}")
  public ResponseEntity<ByteArrayResource> read(
      @PathVariable UUID id, Authentication authentication) {
    var stored = service.read(currentUser.require(authentication), id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(stored.mimeType()))
        .contentLength(stored.bytes().length)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline()
                .filename(stored.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(new ByteArrayResource(stored.bytes()));
  }
}
