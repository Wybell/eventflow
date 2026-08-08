package com.eventflow.auth.application;

import com.eventflow.auth.api.dto.AuthTokenResponse;
import com.eventflow.auth.api.dto.CurrentUserResponse;
import com.eventflow.auth.api.dto.LoginRequest;
import com.eventflow.auth.api.dto.RefreshTokenRequest;
import com.eventflow.auth.infrastructure.persistence.UserAccount;
import com.eventflow.auth.infrastructure.persistence.UserAccountMapper;
import com.eventflow.auth.infrastructure.persistence.UserRoleMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import com.eventflow.shared.security.JwtProperties;
import com.eventflow.shared.security.JwtTokenService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountMapper userAccountMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AuthService(
            UserAccountMapper userAccountMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties,
            Clock clock) {
        this.userAccountMapper = userAccountMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        UserAccount user = userAccountMapper.findByUsername(request.username().trim());
        if (user == null || !user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        userAccountMapper.updateLastLoginTime(user.getId(), LocalDateTime.now(clock));
        LOG.info("Authenticated user, userId={}", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        Long userId = refreshTokenService.rotate(request.refreshToken());
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null || !user.isActive()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(AuthenticatedPrincipal principal) {
        UserAccount user = userAccountMapper.selectById(principal.userId());
        if (user == null || !user.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getMobile(),
                user.getEmail(),
                user.getOrganizationId(),
                Set.copyOf(userRoleMapper.findRoleCodesByUserId(user.getId())));
    }

    private AuthTokenResponse issueTokens(UserAccount user) {
        List<String> roleCodes = userRoleMapper.findRoleCodesByUserId(user.getId());
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(user.getId(), user.getOrganizationId(), Set.copyOf(roleCodes));
        return new AuthTokenResponse(
                jwtTokenService.issueAccessToken(principal),
                refreshTokenService.issue(user.getId()),
                "Bearer",
                jwtProperties.accessTokenTtl().toSeconds());
    }
}
