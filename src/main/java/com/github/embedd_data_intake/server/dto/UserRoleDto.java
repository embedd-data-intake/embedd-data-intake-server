package com.github.embedd_data_intake.server.dto;

import com.github.embedd_data_intake.server.enums.DeviceRole;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserRoleDto {
    private String email;
    private DeviceRole role;
}
