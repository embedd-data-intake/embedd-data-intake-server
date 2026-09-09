package com.github.embedd_data_intake.server.dto;

import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.model.UserDevice;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserDeviceDto {
    private UUID userId;
    private UUID deviceId;
    private DeviceRole role;

    public UserDeviceDto(UserDevice userDevice) {
        this.userId = userDevice.getUserId();
        this.deviceId = userDevice.getDeviceId();
        this.role = userDevice.getRole();
    }
}
