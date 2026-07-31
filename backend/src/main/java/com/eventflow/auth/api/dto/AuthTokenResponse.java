package com.eventflow.auth.api.dto;

public record AuthTokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
