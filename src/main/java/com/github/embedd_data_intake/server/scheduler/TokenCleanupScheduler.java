package com.github.embedd_data_intake.server.scheduler;

import com.github.embedd_data_intake.server.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class TokenCleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository; // TODO: User service

    public TokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void removeExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
    }
}
