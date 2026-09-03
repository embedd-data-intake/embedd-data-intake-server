package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.repository.SensorDataRepository;
import org.springframework.stereotype.Service;

@Service
public class SensorDataService {
    private final SensorDataRepository sensorDataRepository;

    public SensorDataService(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }
}
