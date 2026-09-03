package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.model.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, UUID> {
    // Distinct attributes linked to active device
    List<Attribute> findDistinctAttributesByDeviceId(@Param("deviceId") UUID deviceId);

    // Distinct attributes linked to active devices owned by a user
    @Query("SELECT DISTINCT sd.attribute FROM SensorData sd " +
            "JOIN UserDevice ud ON sd.id.deviceId = ud.device.id " +
            "WHERE ud.user.id = :userId AND ud.deletedAt IS NULL")
    List<Attribute> findDistinctAttributesByUserId(@Param("userId") UUID userId);
}
