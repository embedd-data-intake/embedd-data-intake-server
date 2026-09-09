package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.UserDeviceDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.BadRequestException;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.model.UserDevice;
import com.github.embedd_data_intake.server.repository.UserDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserDeviceService {
    private final UserDeviceRepository userDeviceRepository;

    public UserDeviceService(UserDeviceRepository userDeviceRepository) {
        this.userDeviceRepository = userDeviceRepository;
    }

    @Transactional
    public void addDeviceToUser(UUID userId, UUID deviceId, DeviceRole deviceRole) {
        UserDevice userDevice = new UserDevice();
        userDevice.setUserId(userId);
        userDevice.setDeviceId(deviceId);
        userDevice.setRole(deviceRole);
        userDeviceRepository.save(userDevice);
    }

    @Transactional
    public void grantAccess(UUID userId, UUID deviceId, DeviceRole role) {
        UserDevice userDevice = userDeviceRepository.findByUserIdAndDevice_Id(userId, deviceId)
                .orElseGet(() -> {
                    UserDevice ud = new UserDevice();
                    ud.setUserId(userId);
                    ud.setDeviceId(deviceId);
                    return ud;
                });

        // TODO: Add check/handling if user wants to add an owner
        // TODO: Add recipient confirmation

        userDevice.setRole(role);
        userDevice.setDeletedAt(null);
        userDeviceRepository.save(userDevice);
    }

    @Transactional
    public void removeAccess(UUID userId, UUID deviceId) {
        UserDevice userDevice = userDeviceRepository.findByUserIdAndDevice_Id(userId, deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found."));

        if (Objects.equals(userDevice.getRole(), DeviceRole.OWNER)) {
            throw new BadRequestException("Cannot remove the owner of the device.");
        }

        userDevice.setDeletedAt(OffsetDateTime.now());
        userDeviceRepository.save(userDevice);
    }

    public List<UserDeviceDto> findByUserId(UUID userId) {
        List<UserDevice> userDevice = userDeviceRepository.findByUserId(userId);

        return userDevice.stream().map(UserDeviceDto::new).toList();
    }

    public List<UserDeviceDto> findByUserIdAndRole(UUID userId, DeviceRole role) {
        List<UserDevice> userDevice = userDeviceRepository.findByUserIdAndRole(userId, role);

        return userDevice.stream().map(UserDeviceDto::new).toList();
    }
}
