package com.gymrats.api.feed.application;

import com.gymrats.api.shared.application.MediaStorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.media.provider", havingValue = "local", matchIfMissing = true)
public class LocalMediaStorage implements MediaStorage {
  private final Path storageRoot;

  public LocalMediaStorage(@Value("${app.media.storage-path}") String storagePath) {
    this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
  }

  @Override
  public void write(String key, byte[] bytes, String contentType) {
    try {
      Files.createDirectories(storageRoot);
      Files.write(resolve(key), bytes, StandardOpenOption.CREATE_NEW);
    } catch (IOException exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public byte[] read(String key) {
    try {
      return Files.readAllBytes(resolve(key));
    } catch (IOException exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException exception) {
      throw unavailable(exception);
    }
  }

  private Path resolve(String key) {
    Path resolved = storageRoot.resolve(key).normalize();
    if (!resolved.startsWith(storageRoot)) {
      throw new MediaStorageException("Caminho de mídia inválido.", null);
    }
    return resolved;
  }

  private MediaStorageException unavailable(Exception cause) {
    return new MediaStorageException("O serviço de fotos está indisponível.", cause);
  }
}
