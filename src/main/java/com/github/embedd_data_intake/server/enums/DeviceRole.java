package com.github.embedd_data_intake.server.enums;

public enum DeviceRole {
    OWNER(0),
    ADMIN(1),
    WRITE(2),
    READ(3);

    private final int level;

    DeviceRole(int level) {
        this.level = level;
    }

    public boolean hasPermission(DeviceRole requiredRole) {
        return this.level <= requiredRole.level;
    }
}
