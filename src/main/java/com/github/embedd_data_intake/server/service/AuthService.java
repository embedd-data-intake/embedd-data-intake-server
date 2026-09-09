package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.AuthTokensDto;
import com.github.embedd_data_intake.server.dto.RefreshTokenDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.model.RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {
    private final JwtService jwtService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            JwtService jwtService,
            UserService userService,
            RefreshTokenService refreshTokenService
    ) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void register(String rawEmailAddress, String rawPassword) {
        String emailAddress = rawEmailAddress.trim().toLowerCase();
        UserDto newUser = userService.createUser(emailAddress, rawPassword);
    }

    @Transactional
    public AuthTokensDto login(String rawEmailAddress, String rawPassword) {
        String emailAddress = rawEmailAddress.trim().toLowerCase();

        UserDto user = userService.getUser(emailAddress, rawPassword);

        RefreshTokenDto refreshToken = refreshTokenService.createRefreshToken(user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), refreshToken.getId());

        return new AuthTokensDto(accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthTokensDto refresh(UUID refreshToken) {
        RefreshTokenDto tokenRecord = refreshTokenService.verifyExpiration(refreshToken);
        UserDto user = userService.getUserByRefreshToken(refreshToken);
        String newAccessToken = jwtService.generateAccessToken(user.getId(), tokenRecord.getId());

        return new AuthTokensDto(newAccessToken, refreshToken);
    }

    @Transactional
    public void logout(UUID refreshTokenId) {
        refreshTokenService.invalidateTokenId(refreshTokenId);
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenService.invalidateAllTokens(userId);
    }
}
