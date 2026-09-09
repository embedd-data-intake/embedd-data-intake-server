package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.DeviceRoleDto;
import com.github.embedd_data_intake.server.dto.EmailDto;
import com.github.embedd_data_intake.server.dto.UserDeviceDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.model.User;
import com.github.embedd_data_intake.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {
    // TODO: Move unrelated repos to their own services
    private final UserRepository userRepository;

    private final EmailService emailService;
    private final UserDeviceService userDeviceService;

    public UserService(UserRepository userRepository, EmailService emailService, UserDeviceService userDeviceService) {
        this.emailService = emailService;
        this.userDeviceService = userDeviceService;
        this.userRepository = userRepository;
    }

    /**
     * @param userId for which to find the devices
     * @param role (optional) to filter devices by roles
     * @return list of device ids and the role the user has with them
     */
    public List<DeviceRoleDto> getUserDevices(UUID userId, DeviceRole role) {
        List<UserDeviceDto> userDevices;
        if (Objects.isNull(role)) {
            userDevices = userDeviceService.findByUserId(userId);
        } else {
            userDevices = userDeviceService.findByUserIdAndRole(userId, role);
        }

        return userDevices.stream().map(DeviceRoleDto::new).toList();
    }

    public UserDto getUser(UUID userId) {
        EmailDto emailDto = emailService.getEmailByUserId(userId);

        return new UserDto(userId, emailDto.getEmailAddress());
    }

    public UserDto getUser(String emailAddress) {
        User user = userRepository.findByUserEmails_Email_EmailAddress(emailAddress)
                .orElseThrow(() -> new NotFoundException("Email for user not found."));

        return new UserDto(user.getId(), emailAddress);
    }
}
