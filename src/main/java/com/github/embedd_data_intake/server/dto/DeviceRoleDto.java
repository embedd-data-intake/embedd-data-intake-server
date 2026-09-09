package com.github.embedd_data_intake.server.dto;

import com.github.embedd_data_intake.server.enums.DeviceRole;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DeviceRoleDto {
    private UUID deviceId;
    private DeviceRole role;


    public DeviceRoleDto(UserDeviceDto userDeviceDto) {
        this.deviceId = userDeviceDto.getDeviceId();
        this.role = userDeviceDto.getRole();
    }
}
