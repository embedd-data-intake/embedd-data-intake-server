package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.model.Attribute;
import com.github.embedd_data_intake.server.model.SensorData;
import com.github.embedd_data_intake.server.model.SensorDataId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, SensorDataId> {
    // Find all readings for a specific device, ordered by newest first
    Slice<SensorData> findByIdDeviceIdOrderByIdTimestampDesc(UUID deviceId, Pageable pageable);

    // Find readings for a specific device and attribute within a time range
    @Query("SELECT sd FROM SensorData sd " +
            "WHERE sd.id.deviceId = :deviceId " +
            "AND sd.id.attributeId = :attributeId " +
            "AND sd.id.timestamp BETWEEN :start AND :end")
    List<SensorData> findByDeviceAndAttributeInTimeRange(
            @Param("deviceId") UUID deviceId,
            @Param("attributeId") UUID attributeId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    // Earliest row for a deviceId
    Optional<SensorData> findFirstByIdDeviceIdOrderByIdTimestampAsc(UUID deviceId);

    // Latest row for a deviceId
    Optional<SensorData> findFirstByIdDeviceIdOrderByIdTimestampDesc(UUID deviceId);

    // Earliest row for a deviceId + attributeId
    Optional<SensorData> findFirstByIdDeviceIdAndIdAttributeIdOrderByIdTimestampAsc(UUID deviceId, UUID attributeId);

    // Latest row for a deviceId + attributeId
    Optional<SensorData> findFirstByIdDeviceIdAndIdAttributeIdOrderByIdTimestampDesc(UUID deviceId, UUID attributeId);

    // All readings for a userId, ordered by newest first (Paginated)
    @Query("SELECT sd FROM SensorData sd " +
            "JOIN UserDevice ud ON sd.id.deviceId = ud.device.id " +
            "WHERE ud.user.id = :userId AND ud.deletedAt IS NULL " +
            "ORDER BY sd.id.timestamp DESC")
    Slice<SensorData> findByUserIdOrderByIdTimestampDesc(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    // Readings for a userId and attribute within a time range
    @Query("SELECT sd FROM SensorData sd " +
            "JOIN UserDevice ud ON sd.id.deviceId = ud.device.id " +
            "WHERE ud.user.id = :userId AND ud.deletedAt IS NULL " +
            "AND sd.id.attributeId = :attributeId " +
            "AND sd.id.timestamp BETWEEN :start AND :end")
    List<SensorData> findByUserIdAndAttributeIdAndTimestampBetween(
            @Param("userId") UUID userId,
            @Param("attributeId") UUID attributeId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    // Earliest row for a userId
    @Query("SELECT sd FROM SensorData sd " +
            "JOIN UserDevice ud ON sd.id.deviceId = ud.device.id " +
            "WHERE ud.user.id = :userId AND ud.deletedAt IS NULL " +
            "ORDER BY sd.id.timestamp ASC")
    Optional<SensorData> findFirstByUserIdOrderByIdTimestampAsc(@Param("userId") UUID userId);

    // Latest row for a userId
    @Query("SELECT sd FROM SensorData sd " +
            "JOIN UserDevice ud ON sd.id.deviceId = ud.device.id " +
            "WHERE ud.user.id = :userId AND ud.deletedAt IS NULL " +
            "ORDER BY sd.id.timestamp DESC")
    Optional<SensorData> findFirstByUserIdOrderByIdTimestampDesc(@Param("userId") UUID userId);

    // Earliest row for a userId + attributeId
    @Query("SELECT sd FROM SensorData sd " +
            "JOIN UserDevice ud ON sd.id.deviceId = ud.device.id " +
            "WHERE ud.user.id = :userId AND ud.deletedAt IS NULL " +
            "AND sd.id.attributeId = :attributeId " +
            "ORDER BY sd.id.timestamp ASC")
    Optional<SensorData> findFirstByUserIdAndAttributeIdOrderByIdTimestampAsc(
            @Param("userId") UUID userId,
            @Param("attributeId") UUID attributeId
    );

    // Latest row for a userId + attributeId
    @Query("SELECT sd FROM SensorData sd " +
            "JOIN UserDevice ud ON sd.id.deviceId = ud.device.id " +
            "WHERE ud.user.id = :userId AND ud.deletedAt IS NULL " +
            "AND sd.id.attributeId = :attributeId " +
            "ORDER BY sd.id.timestamp DESC")
    Optional<SensorData> findFirstByUserIdAndAttributeIdOrderByIdTimestampDesc(
            @Param("userId") UUID userId,
            @Param("attributeId") UUID attributeId
    );
}
