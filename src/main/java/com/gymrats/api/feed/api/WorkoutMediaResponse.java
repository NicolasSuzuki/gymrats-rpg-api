package com.gymrats.api.feed.api;

import com.gymrats.api.feed.domain.WorkoutMedia;
import java.util.UUID;

public record WorkoutMediaResponse(
    UUID id, String fileName, String mimeType, long size, String url) {
  public static WorkoutMediaResponse from(WorkoutMedia media) {
    return new WorkoutMediaResponse(
        media.getId(),
        media.getOriginalFileName(),
        media.getMimeType(),
        media.getSizeBytes(),
        "/api/v1/media/" + media.getId());
  }
}
