package com.gymrats.api.feed.api;

import java.util.List;

public record FeedPageResponse(
    List<FeedPostResponse> items,
    String nextCursor,
    List<String> availableCategories) {}
