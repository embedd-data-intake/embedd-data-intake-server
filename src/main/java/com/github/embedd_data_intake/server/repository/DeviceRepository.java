package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByDeviceName(String deviceName);

    // Traverses Device -> userDevices -> user -> id (filters out soft-deleted automatically)
    List<Device> findByUserDevices_UserId(UUID userId);

    boolean existsByUserDevices_UserIdAndDeviceName(UUID userId, String deviceName);
}
