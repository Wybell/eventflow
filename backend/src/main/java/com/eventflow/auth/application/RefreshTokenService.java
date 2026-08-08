package com.eventflow.auth.application;

import com.eventflow.auth.infrastructure.persistence.RefreshToken;
import com.eventflow.auth.infrastructure.persistence.RefreshTokenMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenMapper refreshTokenMapper, JwtProperties jwtProperties, Clock clock) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    public String issue(Long userId) {
        String token = createToken();
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(hash(token));
        refreshToken.setExpiresTime(now.plus(jwtProperties.refreshTokenTtl()));
        refreshTokenMapper.insert(refreshToken);
        return token;
    }

    public Long rotate(String token) {
        RefreshToken refreshToken = refreshTokenMapper.findActiveByTokenHash(hash(token), LocalDateTime.now(clock));
        if (refreshToken == null
                || refreshTokenMapper.revokeById(refreshToken.getId(), LocalDateTime.now(clock)) != 1) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return refreshToken.getUserId();
    }

    public void revoke(String token) {
        RefreshToken refreshToken = refreshTokenMapper.findActiveByTokenHash(hash(token), LocalDateTime.now(clock));
        if (refreshToken != null) {
            refreshTokenMapper.revokeById(refreshToken.getId(), LocalDateTime.now(clock));
        }
    }

    private String createToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
