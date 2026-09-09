package com.github.embedd_data_intake.server.dto;

import com.github.embedd_data_intake.server.model.Device;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DeviceDto {
    private UUID id;
    private String deviceName;

    public DeviceDto(Device device) {
        this.id = device.getId();
        this.deviceName = device.getDeviceName();
    }
}
