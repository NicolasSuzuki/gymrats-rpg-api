package com.gymrats.api.feed.application;

import com.gymrats.api.feed.api.WorkoutMediaResponse;
import com.gymrats.api.feed.domain.FeedPostRepository;
import com.gymrats.api.feed.domain.FeedPost;
import com.gymrats.api.feed.domain.WorkoutMedia;
import com.gymrats.api.feed.domain.WorkoutMediaRepository;
import com.gymrats.api.shared.application.DomainConflictException;
import com.gymrats.api.shared.application.MediaStorageException;
import com.gymrats.api.shared.application.ResourceNotFoundException;
import com.gymrats.api.user.domain.User;
import com.gymrats.api.workout.domain.WorkoutSessionRepository;
import com.gymrats.api.workout.domain.WorkoutSessionStatus;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
  private static final long MAX_BYTES = 5L * 1024 * 1024;
  private static final Set<String> ALLOWED_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");
  private static final Map<String, String> EXTENSIONS =
      Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");

  private final WorkoutMediaRepository media;
  private final FeedPostRepository posts;
  private final WorkoutSessionRepository sessions;
  private final MediaStorage storage;

  public MediaService(
      WorkoutMediaRepository media,
      FeedPostRepository posts,
      WorkoutSessionRepository sessions,
      MediaStorage storage) {
    this.media = media;
    this.posts = posts;
    this.sessions = sessions;
    this.storage = storage;
  }

  @Transactional
  public WorkoutMediaResponse attach(User user, UUID sessionId, MultipartFile file) {
    var session = sessions.findByIdAndOwnerId(sessionId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Treino concluído não encontrado."));
    if (session.getStatus() != WorkoutSessionStatus.COMPLETED) {
      throw new DomainConflictException("A foto só pode ser adicionada a um treino concluído.");
    }
    byte[] bytes = readUpload(file);
    validate(file, bytes);
    String key = UUID.randomUUID() + EXTENSIONS.get(file.getContentType());
    storage.write(key, bytes, file.getContentType());

    var existing = media.findBySessionId(sessionId).orElse(null);
    String previousKey = existing == null ? null : existing.getStorageKey();
    String fileName = safeFileName(file.getOriginalFilename());
    String sha256 = sha256(bytes);
    try {
      if (existing == null) {
        existing = new WorkoutMedia(
            user, session, key, fileName, file.getContentType(), bytes.length, sha256);
      } else {
        existing.replace(key, fileName, file.getContentType(), bytes.length, sha256);
      }
      var saved = media.save(existing);
      posts.findByWorkoutSessionId(sessionId).ifPresent(post -> post.attachMedia(saved));
      if (previousKey != null) deleteQuietly(previousKey);
      return WorkoutMediaResponse.from(saved);
    } catch (RuntimeException exception) {
      deleteQuietly(key);
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public StoredMedia read(User user, UUID mediaId) {
    var stored = media.findById(mediaId)
        .orElseThrow(() -> new ResourceNotFoundException("Foto não encontrada."));
    boolean published = posts.findByMediaId(mediaId).isPresent();
    if (!published && !stored.getOwner().getId().equals(user.getId())) {
      throw new AccessDeniedException("Foto não disponível para este usuário.");
    }
    try {
      return new StoredMedia(
          storage.read(stored.getStorageKey()),
          stored.getMimeType(),
          stored.getOriginalFileName());
    } catch (MediaStorageException exception) {
      throw new MediaStorageException("O serviço de fotos está indisponível.", exception);
    }
  }

  @Transactional
  public void remove(User user, UUID sessionId) {
    var stored = media.findBySessionId(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Foto não encontrada."));
    if (!stored.getOwner().getId().equals(user.getId())) {
      throw new AccessDeniedException("Você não pode remover esta foto.");
    }
    posts.findByWorkoutSessionId(sessionId).ifPresent(post -> post.clearMedia());
    media.delete(stored);
    deleteQuietly(stored.getStorageKey());
  }

  @Transactional
  public void removeAllFor(User user) {
    media.findByOwnerId(user.getId()).forEach(stored -> {
      posts.findByWorkoutSessionId(stored.getSession().getId())
          .ifPresent(FeedPost::clearMedia);
      media.delete(stored);
      deleteQuietly(stored.getStorageKey());
    });
  }

  private byte[] readUpload(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException exception) {
      throw new MediaStorageException("Não foi possível processar a foto.", exception);
    }
  }

  private void validate(MultipartFile file, byte[] bytes) {
    if (bytes.length == 0) throw new DomainConflictException("Selecione uma foto.");
    if (bytes.length > MAX_BYTES) {
      throw new DomainConflictException("A foto deve ter no máximo 5 MB.");
    }
    if (!ALLOWED_TYPES.contains(file.getContentType())
        || !matchesSignature(file.getContentType(), bytes)) {
      throw new DomainConflictException("Use uma imagem JPG, PNG ou WebP válida.");
    }
  }

  private boolean matchesSignature(String type, byte[] bytes) {
    return switch (type) {
      case "image/jpeg" -> bytes.length >= 3
          && (bytes[0] & 0xff) == 0xff
          && (bytes[1] & 0xff) == 0xd8
          && (bytes[2] & 0xff) == 0xff;
      case "image/png" -> bytes.length >= 8
          && (bytes[0] & 0xff) == 0x89
          && bytes[1] == 0x50
          && bytes[2] == 0x4e
          && bytes[3] == 0x47
          && bytes[4] == 0x0d
          && bytes[5] == 0x0a
          && bytes[6] == 0x1a
          && bytes[7] == 0x0a;
      case "image/webp" -> bytes.length >= 12
          && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
          && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
      default -> false;
    };
  }

  private void deleteQuietly(String key) {
    try {
      storage.delete(key);
    } catch (MediaStorageException ignored) {
      // O registro continua sendo a fonte de verdade; órfãos podem ser reconciliados depois.
    }
  }

  private String safeFileName(String name) {
    if (name == null || name.isBlank()) return "foto";
    String safe = name.replace('\\', '/');
    safe = safe.substring(safe.lastIndexOf('/') + 1)
        .replace('\r', '_')
        .replace('\n', '_')
        .replaceAll("[:*?\"<>|]", "_");
    if (safe.isBlank()) safe = "foto";
    return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 indisponível.", exception);
    }
  }

  public record StoredMedia(byte[] bytes, String mimeType, String fileName) {}
}
