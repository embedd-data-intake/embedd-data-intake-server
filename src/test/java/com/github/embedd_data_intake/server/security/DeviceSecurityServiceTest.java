package com.github.embedd_data_intake.server.security;

import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import com.github.embedd_data_intake.server.service.UserDeviceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceSecurityServiceTest {
    @Mock
    private UserDeviceService userDeviceService;

    @InjectMocks
    private DeviceSecurityService deviceSecurityService;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // Helper method to set an authenticated user in SecurityContext
    private void setAuthenticatedUser(UUID userId) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // Helper method to set an anonymous user in SecurityContext
    private void setAnonymousUser() {
        Authentication auth = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // Helper method to set an unauthenticated token in SecurityContext
    private void setUnauthenticatedUser(UUID userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null);
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("hasPermission()")
    class HasPermissionTests {

        @Test
        void hasPermission_WhenUserIsAuthenticatedAndHasPermission_ReturnsTrue() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            String roleName = "ADMIN";

            setAuthenticatedUser(userId);
            given(userDeviceService.userHasPermission(userId, deviceId, DeviceRole.ADMIN)).willReturn(true);

            // Act
            boolean result = deviceSecurityService.hasPermission(deviceId, roleName);

            // Assert
            assertThat(result).isTrue();
            verify(userDeviceService).userHasPermission(userId, deviceId, DeviceRole.ADMIN);
        }

        @Test
        void hasPermission_WhenUserIsAuthenticatedAndLacksPermission_ReturnsFalse() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            String roleName = "OWNER";

            setAuthenticatedUser(userId);
            given(userDeviceService.userHasPermission(userId, deviceId, DeviceRole.OWNER)).willReturn(false);

            // Act
            boolean result = deviceSecurityService.hasPermission(deviceId, roleName);

            // Assert
            assertThat(result).isFalse();
            verify(userDeviceService).userHasPermission(userId, deviceId, DeviceRole.OWNER);
        }

        @Test
        void hasPermission_WhenSecurityContextHasNoAuthentication_ThrowsUnauthorizedException() {
            // Arrange
            UUID deviceId = UUID.randomUUID();
            SecurityContextHolder.getContext().setAuthentication(null);

            // Act & Assert
            assertThatThrownBy(() -> deviceSecurityService.hasPermission(deviceId, "READ"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("User is not logged in.");

            verify(userDeviceService, never()).userHasPermission(any(), any(), any());
        }

        @Test
        void hasPermission_WhenAuthenticationIsNotAuthenticated_ThrowsUnauthorizedException() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            setUnauthenticatedUser(userId);

            // Act & Assert
            assertThatThrownBy(() -> deviceSecurityService.hasPermission(deviceId, "READ"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("User is not logged in.");

            verify(userDeviceService, never()).userHasPermission(any(), any(), any());
        }

        @Test
        void hasPermission_WhenUserIsAnonymous_ThrowsUnauthorizedException() {
            // Arrange
            UUID deviceId = UUID.randomUUID();
            setAnonymousUser();

            // Act & Assert
            assertThatThrownBy(() -> deviceSecurityService.hasPermission(deviceId, "READ"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("User is not logged in.");

            verify(userDeviceService, never()).userHasPermission(any(), any(), any());
        }

        @Test
        void hasPermission_WhenInvalidDeviceRoleProvided_ThrowsIllegalArgumentException() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            setAuthenticatedUser(userId);

            assertThatThrownBy(() -> deviceSecurityService.hasPermission(deviceId, "NON_EXISTENT_ROLE"))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userDeviceService, never()).userHasPermission(any(), any(), any());
        }

        @Test
        void hasPermission_WhenUserDeviceServiceThrowsNotFoundException_PropagatesException() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            setAuthenticatedUser(userId);

            given(userDeviceService.userHasPermission(userId, deviceId, DeviceRole.READ))
                    .willThrow(new NotFoundException("Device not found."));

            assertThatThrownBy(() -> deviceSecurityService.hasPermission(deviceId, "READ"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Device not found.");
        }
    }
}