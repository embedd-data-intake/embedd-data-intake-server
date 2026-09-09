package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import com.github.embedd_data_intake.server.model.RefreshToken;
import com.github.embedd_data_intake.server.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationDays;

    private final UserService userService;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-expiration-days:30}") long expirationDays, UserService userService) {
        this.userService = userService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationDays = expirationDays;
    }

    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {
        UserDto user = userService.getUser(userId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(UUID.randomUUID());
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(this.expirationDays));

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyExpiration(UUID token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token expired. Please sign in again."));

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token expired. Please sign in again.");
        }
        return refreshToken;
    }

    @Transactional
    public void invalidateTokenId(UUID token) {
        refreshTokenRepository.deleteById(token);
    }

    @Transactional
    public void invalidateAllTokens(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
