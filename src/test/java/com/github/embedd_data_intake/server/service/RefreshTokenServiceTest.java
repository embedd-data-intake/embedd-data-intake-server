package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.RefreshTokenDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import com.github.embedd_data_intake.server.model.RefreshToken;
import com.github.embedd_data_intake.server.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserService userService;

    private RefreshTokenService refreshTokenService;

    private static final long EXPIRATION_DAYS = 30L;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, EXPIRATION_DAYS, userService);
    }

    private RefreshToken createRefreshToken(UUID id, UUID userId, UUID tokenValue, OffsetDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        ReflectionTestUtils.setField(token, "id", id);
        token.setUserId(userId);
        token.setToken(tokenValue);
        token.setExpiresAt(expiresAt);
        return token;
    }

    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshTokenTests {
        @Test
        void createRefreshToken_WhenUserExists_SavesAndReturnsRefreshTokenDto() {
            UUID userId = UUID.randomUUID();
            UUID generatedId = UUID.randomUUID();
            UserDto mockUser = new UserDto(userId, "user@example.com");

            given(userService.getUser(userId)).willReturn(mockUser);

            doAnswer(invocation -> {
                RefreshToken savedToken = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedToken, "id", generatedId);
                return savedToken;
            }).when(refreshTokenRepository).save(any(RefreshToken.class));

            RefreshTokenDto result = refreshTokenService.createRefreshToken(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(generatedId);
            assertThat(result.getToken()).isNotNull();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken capturedToken = captor.getValue();
            assertThat(capturedToken.getUserId()).isEqualTo(userId);
            assertThat(capturedToken.getExpiresAt()).isAfter(OffsetDateTime.now());
            assertThat(capturedToken.getExpiresAt()).isBeforeOrEqualTo(OffsetDateTime.now().plusDays(EXPIRATION_DAYS).plusSeconds(5));
        }

        @Test
        void createRefreshToken_WhenUserNotFound_ThrowsNotFoundException() {
            UUID userId = UUID.randomUUID();
            given(userService.getUser(userId)).willThrow(new NotFoundException("User not found."));

            assertThatThrownBy(() -> refreshTokenService.createRefreshToken(userId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found.");

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("verifyExpiration()")
    class VerifyExpirationTests {
        @Test
        void verifyExpiration_WhenTokenIsValidAndActive_ReturnsRefreshTokenDto() throws UnauthorizedException {
            UUID tokenValue = UUID.randomUUID();
            UUID tokenId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            OffsetDateTime futureExpiration = OffsetDateTime.now().plusDays(5);

            RefreshToken validToken = createRefreshToken(tokenId, userId, tokenValue, futureExpiration);

            given(refreshTokenRepository.findByToken(tokenValue)).willReturn(Optional.of(validToken));

            RefreshTokenDto result = refreshTokenService.verifyExpiration(tokenValue);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(tokenId);
            assertThat(result.getToken()).isEqualTo(tokenValue);

            verify(refreshTokenRepository, never()).delete(any());
        }

        @Test
        void verifyExpiration_WhenTokenNotFound_ThrowsUnauthorizedException() {
            UUID tokenValue = UUID.randomUUID();
            given(refreshTokenRepository.findByToken(tokenValue)).willReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(tokenValue))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token expired or invalid. Please sign in.");

            verify(refreshTokenRepository, never()).delete(any());
        }

        @Test
        void verifyExpiration_WhenTokenIsExpired_DeletesTokenAndThrowsUnauthorizedException() {
            UUID tokenValue = UUID.randomUUID();
            UUID tokenId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            OffsetDateTime pastExpiration = OffsetDateTime.now().minusDays(1);

            RefreshToken expiredToken = createRefreshToken(tokenId, userId, tokenValue, pastExpiration);

            given(refreshTokenRepository.findByToken(tokenValue)).willReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(tokenValue))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token expired or invalid. Please sign in.");

            verify(refreshTokenRepository).delete(expiredToken);
        }
    }

    @Nested
    @DisplayName("Token Invalidation & Housekeeping Operations")
    class InvalidationAndUtilityTests {
        @Test
        void invalidateTokenId_DeletesById() {
            UUID tokenId = UUID.randomUUID();

            refreshTokenService.invalidateTokenId(tokenId);

            verify(refreshTokenRepository).deleteById(tokenId);
        }

        @Test
        void invalidateAllTokens_DeletesByUserId() {
            UUID userId = UUID.randomUUID();

            refreshTokenService.invalidateAllTokens(userId);

            verify(refreshTokenRepository).deleteByUserId(userId);
        }

        @Test
        void existsById_WhenExists_ReturnsTrue() {
            UUID tokenId = UUID.randomUUID();
            given(refreshTokenRepository.existsById(tokenId)).willReturn(true);

            boolean exists = refreshTokenService.existsById(tokenId);

            assertThat(exists).isTrue();
            verify(refreshTokenRepository).existsById(tokenId);
        }

        @Test
        void existsById_WhenDoesNotExist_ReturnsFalse() {
            UUID tokenId = UUID.randomUUID();
            given(refreshTokenRepository.existsById(tokenId)).willReturn(false);

            boolean exists = refreshTokenService.existsById(tokenId);

            assertThat(exists).isFalse();
            verify(refreshTokenRepository).existsById(tokenId);
        }

        @Test
        void deleteExpiredRefreshTokens_DeletesTokensExpiredBeforeNow() {
            refreshTokenService.deleteExpiredRefreshTokens();

            verify(refreshTokenRepository).deleteByExpiresAtBefore(any(OffsetDateTime.now().getClass()));
        }
    }
}