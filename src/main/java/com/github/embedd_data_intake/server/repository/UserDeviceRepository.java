package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.model.UserDevice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {
    @EntityGraph(attributePaths = {"device"})
    List<UserDevice> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"device"})
    List<UserDevice> findByUserIdAndRole(UUID userId, DeviceRole role);

    Optional<UserDevice> findByUserIdAndDeviceId(UUID userId, UUID deviceId);

    void deleteByUserIdAndDevice_Id(UUID userId, UUID deviceId);

    @EntityGraph(attributePaths = {"device"})
    List<UserDevice> findByDeviceId(UUID deviceId);

    @EntityGraph(attributePaths = {"device"})
    List<UserDevice> findByDeviceIdAndRole(UUID deviceId, DeviceRole role);
}
