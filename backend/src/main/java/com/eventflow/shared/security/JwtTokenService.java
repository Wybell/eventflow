package com.eventflow.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenService.class);
    private static final String ORGANIZATION_ID_CLAIM = "organizationId";
    private static final String ROLES_CLAIM = "roles";

    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String issueAccessToken(AuthenticatedPrincipal principal) {
        Instant issuedAt = clock.instant();
        JwtBuilder builder = Jwts.builder()
                .issuer(properties.issuer())
                .subject(Long.toString(principal.userId()))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(properties.accessTokenTtl())))
                .claim(ROLES_CLAIM, principal.roles());
        if (principal.organizationId() != null) {
            builder.claim(ORGANIZATION_ID_CLAIM, principal.organizationId());
        }
        return builder.signWith(signingKey()).compact();
    }

    public Optional<AuthenticatedPrincipal> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(toPrincipal(claims));
        } catch (JwtException | IllegalArgumentException exception) {
            LOG.debug("Rejected invalid access token", exception);
            return Optional.empty();
        }
    }

    private AuthenticatedPrincipal toPrincipal(Claims claims) {
        long userId = Long.parseLong(claims.getSubject());
        Long organizationId = claims.get(ORGANIZATION_ID_CLAIM, Long.class);
        return new AuthenticatedPrincipal(userId, organizationId, readRoles(claims.get(ROLES_CLAIM)));
    }

    private Set<String> readRoles(Object rolesClaim) {
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return Set.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }

    private SecretKey signingKey() {
        if (!StringUtils.hasText(properties.secret())) {
            throw new IllegalStateException(
                    "EVENTFLOW_JWT_SECRET must be configured before issuing or accepting tokens.");
        }
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }
}
