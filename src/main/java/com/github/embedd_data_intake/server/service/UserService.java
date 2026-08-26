package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.DeviceRoleDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.model.Email;
import com.github.embedd_data_intake.server.model.UserDevice;
import com.github.embedd_data_intake.server.repository.EmailRepository;
import com.github.embedd_data_intake.server.repository.UserDeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {
    private final EmailRepository emailRepository;
    private final UserDeviceRepository userDeviceRepository;

    public UserService(EmailRepository emailRepository, UserDeviceRepository userDeviceRepository) {
        this.emailRepository = emailRepository;
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

    public UserDto getUser(UUID userId) {
        Email emailAddress = emailRepository.findByUserEmails_UserId(userId)
                .orElseThrow(() -> new NotFoundException("Email for user not found."));

        return new UserDto(userId, emailAddress.getEmailAddress());
    }
}
