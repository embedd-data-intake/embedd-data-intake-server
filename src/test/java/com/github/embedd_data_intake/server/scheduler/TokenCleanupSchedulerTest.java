package com.github.embedd_data_intake.server.scheduler;

import com.github.embedd_data_intake.server.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private TokenCleanupScheduler tokenCleanupScheduler;

    @Nested
    @DisplayName("removeExpiredTokens()")
    class RemoveExpiredTokensTests {
        @Test
        void removeExpiredTokens_DelegatesToRefreshTokenService() {
            tokenCleanupScheduler.removeExpiredTokens();

            verify(refreshTokenService).deleteExpiredRefreshTokens();
        }

        @Test
        void removeExpiredTokens_WhenServiceThrowsException_PropagatesException() {
            doThrow(new RuntimeException("Database error during cleanup"))
                    .when(refreshTokenService).deleteExpiredRefreshTokens();

            assertThatThrownBy(() -> tokenCleanupScheduler.removeExpiredTokens())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error during cleanup");

            verify(refreshTokenService).deleteExpiredRefreshTokens();
        }
    }
}