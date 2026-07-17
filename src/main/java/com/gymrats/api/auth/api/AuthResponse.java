package com.gymrats.api.auth.api;
public record AuthResponse(String accessToken, String tokenType, long expiresIn) {}
