package com.github.embedd_data_intake.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String TEST_SECRET = "super_secret_jwt_key_that_is_at_least_32_bytes_long!!";
    private static final int EXPIRATION_DAYS = 1;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_DAYS);
    }

    @Nested
    @DisplayName("Token Generation & Payload Extraction")
    class GenerationAndExtractionTests {
        @Test
        void generateAccessToken_CreatesValidJwtWithClaims() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();

            String token = jwtService.generateAccessToken(userId, sessionId);

            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
            assertThat(jwtService.extractSessionId(token)).isEqualTo(sessionId);
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        void extractUserId_WhenTokenMalformed_ThrowsException() {
            String malformedToken = "invalid.jwt.token";

            assertThatThrownBy(() -> jwtService.extractUserId(malformedToken))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class ValidationTests {
        @Test
        void isTokenValid_WhenTokenValid_ReturnsTrue() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String token = jwtService.generateAccessToken(userId, sessionId);

            boolean isValid = jwtService.isTokenValid(token);

            assertThat(isValid).isTrue();
        }

        @Test
        void isTokenValid_WhenTokenTamperedWith_ReturnsFalse() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String validToken = jwtService.generateAccessToken(userId, sessionId);
            String tamperedToken = validToken + "tampered";

            boolean isValid = jwtService.isTokenValid(tamperedToken);

            assertThat(isValid).isFalse();
        }

        @Test
        void isTokenValid_WhenSignedWithDifferentSecret_ReturnsFalse() {
            String otherSecret = "another_different_secret_key_that_is_32_bytes!!";
            JwtService otherJwtService = new JwtService(otherSecret, EXPIRATION_DAYS);

            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String tokenFromOtherKey = otherJwtService.generateAccessToken(userId, sessionId);

            boolean isValid = jwtService.isTokenValid(tokenFromOtherKey);

            assertThat(isValid).isFalse();
        }

        @Test
        void isTokenValid_WhenTokenExpired_ReturnsFalse() {
            JwtService expiredJwtService = new JwtService(TEST_SECRET, -1);
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();

            String expiredToken = expiredJwtService.generateAccessToken(userId, sessionId);

            boolean isValid = jwtService.isTokenValid(expiredToken);

            assertThat(isValid).isFalse();
        }

        @Test
        void isTokenValid_WhenTokenNullOrEmpty_ReturnsFalse() {
            assertThat(jwtService.isTokenValid(null)).isFalse();
            assertThat(jwtService.isTokenValid("")).isFalse();
        }
    }
}