package com.anbuz.domain.user.service;

import com.anbuz.domain.user.service.impl.AuthService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthService token scenarios")
class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "secret", "unit-test-secret");
        ReflectionTestUtils.setField(authService, "expireDays", 7);
    }

    @Nested
    @DisplayName("generate and parse token")
    class GenerateAndParseToken {

        @Test
        @DisplayName("returns a token that can be parsed back to the same user id")
        void givenUserId_whenGenerateToken_thenCanParseTheSameUserId() {
            String token = authService.generateToken(100L);

            assertThat(authService.parseUserId(token)).isEqualTo(100L);
        }

        @Test
        @DisplayName("throws when parsing an invalid token")
        void givenInvalidToken_whenParseUserId_thenThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> authService.parseUserId("invalid-token"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("check token expiration")
    class CheckTokenExpiration {

        @Test
        @DisplayName("returns false for a fresh token")
        void givenFreshToken_whenIsExpiringSoon_thenReturnsFalse() {
            String token = authService.generateToken(200L);

            assertThat(authService.isExpiringSoon(token)).isFalse();
        }

        @Test
        @DisplayName("returns true for a token expiring within one day")
        void givenSoonExpiringToken_whenIsExpiringSoon_thenReturnsTrue() {
            String token = buildTokenWithExpiry(Duration.ofHours(12));

            assertThat(authService.isExpiringSoon(token)).isTrue();
        }

        @Test
        @DisplayName("returns false for an invalid token")
        void givenInvalidToken_whenIsExpiringSoon_thenReturnsFalse() {
            assertThat(authService.isExpiringSoon("broken-token")).isFalse();
        }
    }

    private String buildTokenWithExpiry(Duration duration) {
        long now = System.currentTimeMillis();
        return JWT.create()
                .withClaim("userId", 1L)
                .withIssuedAt(new Date(now))
                .withExpiresAt(new Date(now + duration.toMillis()))
                .sign(Algorithm.HMAC256("unit-test-secret"));
    }

}
