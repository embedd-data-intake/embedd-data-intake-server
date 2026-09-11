package com.github.embedd_data_intake.server.scheduler;

import com.github.embedd_data_intake.server.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenCleanupScheduler {
    private final RefreshTokenService refreshTokenService;

    public TokenCleanupScheduler(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void removeExpiredTokens() {
        refreshTokenService.deleteExpiredRefreshTokens();
    }
}
