package com.github.embedd_data_intake.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.embedd_data_intake.server.config.SecurityConfig;
import com.github.embedd_data_intake.server.dto.AuthRequestDto;
import com.github.embedd_data_intake.server.dto.AuthTokensDto;
import com.github.embedd_data_intake.server.dto.RefreshRequestDto;
import com.github.embedd_data_intake.server.exceptions.impl.ConflictException;
import com.github.embedd_data_intake.server.exceptions.impl.UnauthorizedException;
import com.github.embedd_data_intake.server.handler.RestExceptionHandler;
import com.github.embedd_data_intake.server.service.AuthService;
import com.github.embedd_data_intake.server.service.JwtService;
import com.github.embedd_data_intake.server.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({RestExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    private AuthRequestDto authRequest;

    private UUID userId;
    private UUID sessionId;
    private String bearerHeader;

    @BeforeEach
    void setUp() {
        authRequest = new AuthRequestDto();
        authRequest.setEmail("user@example.com");
        authRequest.setPassword("securePassword123");

        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        bearerHeader = "Bearer " + UUID.randomUUID();
    }

    void authenticatedSetup() {
        when(jwtService.extractSessionId(anyString())).thenReturn(sessionId);
        when(refreshTokenService.existsById(sessionId)).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(userId);
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {
        @Test
        @DisplayName("Should return 201 Created on successful registration")
        void register_Success() throws Exception {
            willDoNothing().given(authService).register(authRequest.getEmail(), authRequest.getPassword());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isCreated());

            verify(authService).register(authRequest.getEmail(), authRequest.getPassword());
        }

        @Test
        @DisplayName("Should return 409 Conflict when user already exists")
        void register_UserAlreadyExists() throws Exception {
            willThrow(new ConflictException("Email already in use"))
                    .given(authService).register(authRequest.getEmail(), authRequest.getPassword());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", is("Email already in use")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {
        @Test
        @DisplayName("Should return 200 OK with AuthTokensDto on successful authentication")
        void login_Success() throws Exception {
            UUID refreshToken = UUID.randomUUID();
            AuthTokensDto tokens = new AuthTokensDto("access.token.jwt", refreshToken);
            given(authService.login(authRequest.getEmail(), authRequest.getPassword())).willReturn(tokens);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", is("access.token.jwt")))
                    .andExpect(jsonPath("$.refreshToken", is(refreshToken.toString())));

            verify(authService).login(authRequest.getEmail(), authRequest.getPassword());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized on invalid credentials")
        void login_InvalidCredentials() throws Exception {
            given(authService.login(authRequest.getEmail(), authRequest.getPassword()))
                    .willThrow(new UnauthorizedException("Invalid email or password"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", is("Invalid email or password")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTests {
        @Test
        @DisplayName("Should return 200 OK with new tokens on valid refresh request")
        void refresh_Success() throws Exception {
            UUID refreshToken = UUID.randomUUID();

            RefreshRequestDto refreshRequest = new RefreshRequestDto();
            refreshRequest.setRefreshToken(refreshToken);

            AuthTokensDto newTokens = new AuthTokensDto("new.access.token", refreshToken);
            given(authService.refresh(refreshToken)).willReturn(newTokens);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", is("new.access.token")))
                    .andExpect(jsonPath("$.refreshToken", is(refreshToken.toString())));

            verify(authService).refresh(refreshToken);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when refresh token is expired or invalid")
        void refresh_InvalidToken() throws Exception {
            UUID invalidRefreshToken = UUID.randomUUID();

            RefreshRequestDto refreshRequest = new RefreshRequestDto();
            refreshRequest.setRefreshToken(invalidRefreshToken);

            given(authService.refresh(invalidRefreshToken))
                    .willThrow(new UnauthorizedException("Refresh token expired"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", is("Refresh token expired")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {
        @Test
        @WithUserDetails
        @DisplayName("Should extract session ID and return 204 No Content")
        void logout_Success() throws Exception {
            authenticatedSetup();
            given(jwtService.extractSessionId(anyString())).willReturn(sessionId);
            willDoNothing().given(authService).logout(sessionId);

            mockMvc.perform(post("/api/v1/auth/logout")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader))
                    .andExpect(status().isNoContent());

            verify(jwtService, times(2)).extractSessionId(anyString());
            verify(authService).logout(sessionId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout-all")
    class LogoutAllTests {
        @Test
        @WithUserDetails
        @DisplayName("Should invalidate all tokens for principal user and return 204 No Content")
        void logoutAll_Success() throws Exception {
            authenticatedSetup();
            willDoNothing().given(authService).logoutAll(userId);

            mockMvc.perform(post("/api/v1/auth/logout-all")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                            .principal(() -> userId.toString()))
                    .andExpect(status().isNoContent());

            verify(authService).logoutAll(userId);
        }
    }
}