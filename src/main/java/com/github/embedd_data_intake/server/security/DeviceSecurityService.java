package com.github.embedd_data_intake.server.security;

import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.repository.UserDeviceRepository;
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

    public boolean hasPermission(UUID deviceId, String requiredRoleName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(auth) || !auth.isAuthenticated()) {
            return false;
        }

        UUID currentUserId = (UUID) auth.getPrincipal();
        DeviceRole requiredRole = DeviceRole.valueOf(requiredRoleName);

        return userDeviceRepository.findByUserIdAndDevice_Id(currentUserId, deviceId)
                .map(role -> role.getRole().hasPermission(requiredRole))
                .orElse(false);
    }
}
