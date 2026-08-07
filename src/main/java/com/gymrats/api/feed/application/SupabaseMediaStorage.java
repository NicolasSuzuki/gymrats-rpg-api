package com.gymrats.api.feed.application;

import com.gymrats.api.shared.application.MediaStorageException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.media.provider", havingValue = "supabase")
public class SupabaseMediaStorage implements MediaStorage {
  private final HttpClient client;
  private final URI objectBaseUri;
  private final String serviceRoleKey;

  public SupabaseMediaStorage(
      @Value("${app.media.supabase.url}") String supabaseUrl,
      @Value("${app.media.supabase.bucket}") String bucket,
      @Value("${app.media.supabase.service-role-key}") String serviceRoleKey) {
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    this.objectBaseUri = URI.create(stripTrailingSlash(supabaseUrl)
        + "/storage/v1/object/" + requirePathSegment(bucket) + "/");
    this.serviceRoleKey = serviceRoleKey;
  }

  @Override
  public void write(String key, byte[] bytes, String contentType) {
    var request = authorized(HttpRequest.newBuilder(objectUri(key)))
        .header("Content-Type", contentType)
        .header("x-upsert", "false")
        .POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
    send(request, 200, 201);
  }

  @Override
  public byte[] read(String key) {
    var request = authorized(HttpRequest.newBuilder(objectUri(key))).GET().build();
    return send(request, 200).body();
  }

  @Override
  public void delete(String key) {
    var request = authorized(HttpRequest.newBuilder(objectUri(key))).DELETE().build();
    send(request, 200, 204);
  }

  private HttpRequest.Builder authorized(HttpRequest.Builder builder) {
    return builder.timeout(Duration.ofSeconds(30))
        .header("apikey", serviceRoleKey)
        .header("Authorization", "Bearer " + serviceRoleKey);
  }

  private HttpResponse<byte[]> send(HttpRequest request, int... acceptedStatuses) {
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      for (int accepted : acceptedStatuses) {
        if (response.statusCode() == accepted) return response;
      }
      throw new MediaStorageException(
          "O serviço de fotos recusou a operação (HTTP " + response.statusCode() + ").", null);
    } catch (IOException exception) {
      throw new MediaStorageException("O serviço de fotos está indisponível.", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new MediaStorageException("A operação de fotos foi interrompida.", exception);
    }
  }

  private URI objectUri(String key) {
    return objectBaseUri.resolve(requirePathSegment(key));
  }

  private static String requirePathSegment(String value) {
    if (value == null || !value.matches("[A-Za-z0-9._-]+")) {
      throw new IllegalArgumentException("Bucket ou chave de mídia inválido.");
    }
    return value;
  }

  private static String stripTrailingSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
