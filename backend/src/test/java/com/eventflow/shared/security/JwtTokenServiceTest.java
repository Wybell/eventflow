package com.eventflow.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final String SECRET = "c3VwZXItc2VjdXJlLWxvY2FsLWRldmVsb3BtZW50LXNlY3JldC1rZXktMzItYnl0ZXM=";

    @Test
    void shouldIssueAndParseAnAccessToken() {
        JwtProperties properties = new JwtProperties("eventflow", SECRET, Duration.ofMinutes(30), Duration.ofDays(7));
        Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        JwtTokenService service = new JwtTokenService(properties, clock);
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(42L, 7L, Set.of("ORGANIZER"));

        String token = service.issueAccessToken(principal);

        assertThat(service.parseAccessToken(token)).contains(principal);
    }
}
