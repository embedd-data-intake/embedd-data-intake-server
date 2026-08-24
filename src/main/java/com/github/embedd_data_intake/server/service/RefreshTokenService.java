package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import com.github.embedd_data_intake.server.model.RefreshToken;
import com.github.embedd_data_intake.server.model.User;
import com.github.embedd_data_intake.server.repository.RefreshTokenRepository;
import com.github.embedd_data_intake.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationDays;

    public RefreshTokenService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-expiration-days:30}") long expirationDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationDays = expirationDays;
    }

    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID());
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(this.expirationDays));

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyExpiration(UUID token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token).orElseThrow();

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token expired. Please sign in again.");
        }
        return refreshToken;
    }

    @Transactional
    public void invalidateToken(UUID token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void invalidateAllTokens(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
