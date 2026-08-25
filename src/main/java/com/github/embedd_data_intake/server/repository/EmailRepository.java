package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.dto.UserRoleDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailRepository extends JpaRepository<Email, UUID> {
    Optional<Email> findByEmailAddress(String emailAddress);

    boolean existsByEmailAddress(String emailAddress);

    @Query("""
    SELECT DISTINCT new com.github.embedd_data_intake.server.dto.UserRoleDto(e.emailAddress, ud.role)
    FROM UserDevice ud
    JOIN ud.user u
    JOIN UserEmail ue ON ue.user = u
    JOIN ue.email e
    WHERE ud.device.id = :deviceId
      AND ud.deletedAt IS NULL
      AND ue.deletedAt IS NULL
""")
    List<UserRoleDto> findActiveUserEmailsForDevice(@Param("deviceId") UUID deviceId);

    @Query("""
    SELECT DISTINCT new com.github.embedd_data_intake.server.dto.UserRoleDto(e.emailAddress, ud.role)
    FROM UserDevice ud
    JOIN ud.user u
    JOIN UserEmail ue ON ue.user = u
    JOIN ue.email e
    WHERE ud.device.id = :deviceId
      AND ud.role = :role
      AND ud.deletedAt IS NULL
      AND ue.deletedAt IS NULL
""")
    List<UserRoleDto> findActiveUserEmailsForDeviceAndRole(@Param("deviceId") UUID deviceId, @Param("role") DeviceRole role);
}
