package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.DeviceRoleDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.model.UserDevice;
import com.github.embedd_data_intake.server.repository.UserDeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {
    private final UserDeviceRepository userDeviceRepository;

    public UserService(UserDeviceRepository userDeviceRepository) {
        this.userDeviceRepository = userDeviceRepository;
    }

    /**
     * @param userId for which to find the devices
     * @param role (optional) to filter devices by roles
     * @return list of device ids and the role the user has with them
     */
    public List<DeviceRoleDto> getUserDevices(UUID userId, DeviceRole role) {
        List<UserDevice> userDevices;
        if (Objects.isNull(role)) {
            userDevices = userDeviceRepository.findByUserId(userId);
        } else {
            userDevices = userDeviceRepository.findByUserIdAndRole(userId, role);
        }

        return userDevices.stream()
                .map(userDevice -> new DeviceRoleDto(userDevice.getDevice().getId(), userDevice.getRole()))
                .toList();
    }
}
