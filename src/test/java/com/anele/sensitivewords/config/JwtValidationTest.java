package com.anele.sensitivewords.config;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.assertj.core.api.Assertions.assertThat;

class JwtValidationTest {
    @Test void acceptsIntendedAudienceAndIssuer() {
        assertThat(JwtConfig.validator("https://issuer.example", "sensitivewords-api")
            .validate(token("https://issuer.example", "sensitivewords-api", Instant.now().plusSeconds(300)))
            .hasErrors()).isFalse();
    }
    @Test void rejectsWrongAudience() {
        assertThat(JwtConfig.validator("https://issuer.example", "sensitivewords-api")
            .validate(token("https://issuer.example", "other-api", Instant.now().plusSeconds(300)))
            .hasErrors()).isTrue();
    }
    @Test void rejectsWrongIssuer() {
        assertThat(JwtConfig.validator("https://issuer.example", "sensitivewords-api")
            .validate(token("https://untrusted.example", "sensitivewords-api", Instant.now().plusSeconds(300)))
            .hasErrors()).isTrue();
    }
    @Test void rejectsExpiredTokenBeyondClockSkew() {
        assertThat(JwtConfig.validator("https://issuer.example", "sensitivewords-api")
            .validate(token("https://issuer.example", "sensitivewords-api", Instant.now().minusSeconds(120)))
            .hasErrors()).isTrue();
    }
    private Jwt token(String issuer, String audience, Instant expiry) {
        return Jwt.withTokenValue("test-token").header("alg", "RS256")
            .issuer(issuer).subject("caller").audience(List.of(audience))
            .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expiry).build();
    }
}
