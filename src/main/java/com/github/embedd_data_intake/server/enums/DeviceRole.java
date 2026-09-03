package com.github.embedd_data_intake.server.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DeviceRole {
    OWNER(0),
    ADMIN(1),
    WRITE(2),
    READ(3);

    private final int level;

    public boolean hasPermission(DeviceRole requiredRole) {
        return this.level <= requiredRole.level;
    }
}
