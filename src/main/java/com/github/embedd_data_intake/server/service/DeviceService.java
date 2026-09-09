package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.DeviceDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.dto.UserRoleDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.BadRequestException;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.model.Device;
import com.github.embedd_data_intake.server.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;

    private final UserService userService;
    private final UserDeviceService userDeviceService;
    private final EmailService emailService;

    public DeviceService(DeviceRepository deviceRepository, UserService userService, UserDeviceService userDeviceService, EmailService emailService) {
        this.userService = userService;
        this.deviceRepository = deviceRepository;
        this.userDeviceService = userDeviceService;
        this.emailService = emailService;
    }

    /**
     * @param deviceId of the target device to assign user role
     * @param targetEmail of the user to assign the role
     * @param role to assign to the user
     * @throws NotFoundException if a user or device is not found
     */
    @Transactional
    public void grantAccess(UUID deviceId, String targetEmail, DeviceRole role) throws NotFoundException {
        UserDto targetUser = userService.getUser(targetEmail);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        userDeviceService.grantAccess(targetUser.getId(), deviceId, role);
    }

    /**
     * @param deviceId of the target device to remove the user's access
     * @param targetEmail of the user to remove any access
     * @throws NotFoundException if a user, device or user-device relationship is not found
     * @throws BadRequestException if the user tries to remove an OWNER
     */
    @Transactional
    public void removeAccess(UUID deviceId, String targetEmail) throws NotFoundException, BadRequestException {
        UserDto targetUser = userService.getUser(targetEmail);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        userDeviceService.removeAccess(targetUser.getId(), device.getId());
    }

    /**
     * @param ownerId of the user who owns the device, will be assigned as 'OWNER'
     * @param deviceName which will show up when browsing devices
     * @return the id of the new device
     */
    @Transactional
    public UUID addDevice(UUID ownerId, String deviceName) throws NotFoundException {
        // TODO: Add new device identification fields when implementation of the device is ready
        // TODO: Only allow verified emails to add
        UserDto user = userService.getUser(ownerId);

        Device device = new Device();
        device.setDeviceName(deviceName);
        deviceRepository.save(device);

        userDeviceService.addDeviceToUser(user.getId(), device.getId(), DeviceRole.OWNER);

        return device.getId();
    }

    public DeviceDto getDevice(UUID deviceId) throws NotFoundException {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found."));

        return new DeviceDto(device);
    }

    public List<UserRoleDto> getDeviceAccess(UUID deviceId, DeviceRole role) {
        List<UserRoleDto> userRoles;
        if (Objects.isNull(role)) {
            userRoles = emailService.getAccessForDevice(deviceId);
        } else {
            userRoles = emailService.getAccessForDevice(deviceId, role);
        }

        return userRoles;
    }
}
