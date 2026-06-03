package io.mywallet.auth.interfaces.rest.dto;

public record AuthResponse(
    String accessToken,
    long accessTokenExpiresInSeconds,
    String refreshToken
) {}
