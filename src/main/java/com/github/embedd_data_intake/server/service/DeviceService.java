package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.enums.DeviceRole;
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

    @Transactional
    public void grantAccess(UUID deviceId, String targetEmail, DeviceRole role) {
        User targetUser = userRepository.findByUserEmails_Email_EmailAddress(targetEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        // Check if mapping exists, update or create new
        UserDevice userDevice = userDeviceRepository.findByUserAndDevice_Id(targetUser.getId(), deviceId)
                .orElseGet(() -> {
                    UserDevice ud = new UserDevice();
                    ud.setUser(targetUser);
                    ud.setDevice(device);
                    return ud;
                });

        userDevice.setRole(role);
        userDevice.setDeletedAt(null); // Restore if soft-deleted previously
        userDeviceRepository.save(userDevice);
    }

    @Transactional
    public void removeAccess(UUID deviceId, String targetEmail) {
        User targetUser = userRepository.findByUserEmails_Email_EmailAddress(targetEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        UserDevice userDevice = userDeviceRepository.findByUserAndDevice_Id(targetUser.getId(), deviceId)
                .orElseThrow(() -> new NotFoundException("User does not have access to the device"));

        userDevice.setDeletedAt(OffsetDateTime.now());
        userDeviceRepository.save(userDevice);
    }

    public Object getTelemetry(UUID deviceId) {
        // TODO: Connect to the TimescaleDB
        throw new RuntimeException("Not yet implemented");
    }
}
