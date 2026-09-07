package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.model.SensorData;
import com.github.embedd_data_intake.server.model.SensorDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorDataRepository extends
        JpaRepository<SensorData, SensorDataId>, JpaSpecificationExecutor<SensorData> {}
