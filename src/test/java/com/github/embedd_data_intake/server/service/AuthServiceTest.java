package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.AuthTokensDto;
import com.github.embedd_data_intake.server.dto.RefreshTokenDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.exceptions.ConflictException;
import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        void register_TrimsAndLowercasesEmail_ThenCreatesUser() throws ConflictException {
            String rawEmail = "  User.Name@Example.COM  ";
            String expectedNormalizedEmail = "user.name@example.com";
            String rawPassword = "password123";

            UserDto mockUser = new UserDto(UUID.randomUUID(), expectedNormalizedEmail);
            given(userService.createUser(expectedNormalizedEmail, rawPassword)).willReturn(mockUser);

            authService.register(rawEmail, rawPassword);

            verify(userService).createUser(expectedNormalizedEmail, rawPassword);
        }

        @Test
        void register_WhenUserServiceThrowsConflictException_PropagatesException() throws ConflictException {
            String rawEmail = "existing@example.com";
            String rawPassword = "password123";

            given(userService.createUser(rawEmail, rawPassword))
                    .willThrow(new ConflictException("Email address is already in use."));

            assertThatThrownBy(() -> authService.register(rawEmail, rawPassword))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Email address is already in use.");
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {
        @Test
        void login_WithValidCredentials_ReturnsAuthTokensDto() {
            String rawEmail = "  User@Example.COM  ";
            String expectedEmail = "user@example.com";
            String password = "secretPassword";

            UUID userId = UUID.randomUUID();
            UUID refreshTokenId = UUID.randomUUID();
            UUID refreshTokenValue = UUID.randomUUID();

            UserDto mockUser = new UserDto(userId, expectedEmail);
            RefreshTokenDto mockRefreshToken = new RefreshTokenDto(refreshTokenId, refreshTokenValue);

            given(userService.getUser(expectedEmail, password)).willReturn(mockUser);
            given(refreshTokenService.createRefreshToken(userId)).willReturn(mockRefreshToken);
            given(jwtService.generateAccessToken(userId, refreshTokenId)).willReturn("mocked.jwt.access.token");

            AuthTokensDto result = authService.login(rawEmail, password);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("mocked.jwt.access.token");
            assertThat(result.getRefreshToken()).isEqualTo(refreshTokenValue);

            verify(userService).getUser(expectedEmail, password);
            verify(refreshTokenService).createRefreshToken(userId);
            verify(jwtService).generateAccessToken(userId, refreshTokenId);
        }

        @Test
        void login_WithInvalidCredentials_ThrowsUnauthorizedException() {
            String email = "user@example.com";
            String password = "wrongPassword";

            given(userService.getUser(email, password))
                    .willThrow(new UnauthorizedException("Invalid email or password."));

            assertThatThrownBy(() -> authService.login(email, password))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password.");

            verify(refreshTokenService, never()).createRefreshToken(any());
            verify(jwtService, never()).generateAccessToken(any(), any());
        }
    }

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {
        @Test
        void refresh_WithValidToken_ReturnsNewAuthTokens() {
            UUID refreshTokenValue = UUID.randomUUID();
            UUID refreshTokenId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            RefreshTokenDto mockTokenRecord = new RefreshTokenDto(refreshTokenId, refreshTokenValue);
            UserDto mockUser = new UserDto(userId, "user@example.com");

            given(refreshTokenService.verifyExpiration(refreshTokenValue)).willReturn(mockTokenRecord);
            given(userService.getUserByRefreshToken(refreshTokenValue)).willReturn(mockUser);
            given(jwtService.generateAccessToken(userId, refreshTokenId)).willReturn("new.jwt.access.token");

            AuthTokensDto result = authService.refresh(refreshTokenValue);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("new.jwt.access.token");
            assertThat(result.getRefreshToken()).isEqualTo(refreshTokenValue);

            verify(refreshTokenService).verifyExpiration(refreshTokenValue);
            verify(userService).getUserByRefreshToken(refreshTokenValue);
            verify(jwtService).generateAccessToken(userId, refreshTokenId);
        }

        @Test
        void refresh_WhenTokenExpired_ThrowsUnauthorizedException() {
            UUID refreshTokenValue = UUID.randomUUID();
            given(refreshTokenService.verifyExpiration(refreshTokenValue))
                    .willThrow(new UnauthorizedException("Refresh token was expired. Please make a new signin request"));

            assertThatThrownBy(() -> authService.refresh(refreshTokenValue))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token was expired. Please make a new signin request");

            verify(userService, never()).getUserByRefreshToken(any());
            verify(jwtService, never()).generateAccessToken(any(), any());
        }
    }

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        void logout_InvalidatesTokenId() {
            UUID refreshTokenId = UUID.randomUUID();

            authService.logout(refreshTokenId);

            verify(refreshTokenService).invalidateTokenId(refreshTokenId);
        }

        @Test
        void logoutAll_InvalidatesAllUserTokens() {
            UUID userId = UUID.randomUUID();

            authService.logoutAll(userId);

            verify(refreshTokenService).invalidateAllTokens(userId);
        }
    }
}