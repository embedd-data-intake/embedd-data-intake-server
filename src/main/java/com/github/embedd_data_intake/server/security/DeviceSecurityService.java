package com.github.embedd_data_intake.server.security;

import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import com.github.embedd_data_intake.server.repository.UserDeviceRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component("deviceSecurity")
public class DeviceSecurityService {
    private final UserDeviceRepository userDeviceRepository;


    public DeviceSecurityService(UserDeviceRepository userDeviceRepository) {
        this.userDeviceRepository = userDeviceRepository;
    }

    /**
     * @param deviceId to check if principal is authorized to access a device
     * @param requiredRoleName minimum to access a device
     * @return true if user has the required access to the device
     * @throws NotFoundException if the device does not exist or if user has not access
     * @throws UnauthorizedException if the user is not authenticated
     */
    public boolean hasPermission(UUID deviceId, String requiredRoleName) throws NotFoundException, UnauthorizedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(auth) || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("User is not logged in.");
        }

        UUID currentUserId = (UUID) auth.getPrincipal();
        DeviceRole requiredRole = DeviceRole.valueOf(requiredRoleName);

        // Get the active user-device relationship and check the role of the user for the respective device
        return userDeviceRepository.findByUserIdAndDevice_Id(currentUserId, deviceId)
                .map(role -> role.getRole().hasPermission(requiredRole))
                .orElseThrow(() -> new NotFoundException("Device not found."));
    }
}
