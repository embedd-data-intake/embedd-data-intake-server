package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.DeviceDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.BadRequestException;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.model.Device;
import com.github.embedd_data_intake.server.model.User;
import com.github.embedd_data_intake.server.model.UserDevice;
import com.github.embedd_data_intake.server.repository.DeviceRepository;
import com.github.embedd_data_intake.server.repository.UserDeviceRepository;
import com.github.embedd_data_intake.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class DeviceService {
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final UserDeviceRepository userDeviceRepository;

    public DeviceService(UserRepository userRepository, DeviceRepository deviceRepository, UserDeviceRepository userDeviceRepository) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.userDeviceRepository = userDeviceRepository;
    }

    /**
     * @param deviceId of the target device to assign user role
     * @param targetEmail of the user to assign the role
     * @param role to assign to the user
     * @throws NotFoundException if a user or device is not found
     */
    @Transactional
    public void grantAccess(UUID deviceId, String targetEmail, DeviceRole role) throws NotFoundException {
        User targetUser = userRepository.findByUserEmails_Email_EmailAddress(targetEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        // Check if mapping exists, update or create new
        UserDevice userDevice = userDeviceRepository.findByUserIdAndDevice_Id(targetUser.getId(), deviceId)
                .orElseGet(() -> {
                    UserDevice ud = new UserDevice();
                    ud.setUser(targetUser);
                    ud.setDevice(device);
                    return ud;
                });

        // TODO: Add check/handling if user wants to add an owner

        userDevice.setRole(role);
        userDevice.setDeletedAt(null); // Restore if soft-deleted previously
        userDeviceRepository.save(userDevice);
    }

    /**
     * @param deviceId of the target device to remove the user's access
     * @param targetEmail of the user to remove any access
     * @throws NotFoundException if a user, device or user-device relationship is not found
     * @throws BadRequestException if the user tries to remove an OWNER
     */
    @Transactional
    public void removeAccess(UUID deviceId, String targetEmail) throws NotFoundException, BadRequestException {
        User targetUser = userRepository.findByUserEmails_Email_EmailAddress(targetEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        UserDevice userDevice = userDeviceRepository.findByUserIdAndDevice_Id(targetUser.getId(), deviceId)
                .orElseThrow(() -> new NotFoundException("User does not have access to the device"));

        if (Objects.equals(userDevice.getRole(), DeviceRole.OWNER)) {
            throw new BadRequestException("Cannot remove the owner of the device.");
        }

        userDevice.setDeletedAt(OffsetDateTime.now());
        userDeviceRepository.save(userDevice);
    }

    public Object getTelemetry(UUID deviceId) {
        // TODO: Connect to the TimescaleDB
        throw new RuntimeException("Not yet implemented");
    }

    /**
     * @param ownerId of the user who owns the device, will be assigned as 'OWNER'
     * @param deviceName which will show up when browsing devices
     * @return the id of the new device
     */
    @Transactional
    public UUID addDevice(UUID ownerId, String deviceName) throws NotFoundException {
        // TODO: Add new device identification fields when implementation of the device is ready
        Device device = new Device();
        device.setDeviceName(deviceName);
        deviceRepository.save(device);

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserDevice userDevice = new UserDevice();
        userDevice.setUser(user);
        userDevice.setDevice(device);
        userDevice.setRole(DeviceRole.OWNER);
        userDeviceRepository.save(userDevice);

        return device.getId();
    }

    public DeviceDto getDevice(UUID deviceId) throws NotFoundException {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found."));

        return new DeviceDto(device.getId(), device.getDeviceName());
    }
}
