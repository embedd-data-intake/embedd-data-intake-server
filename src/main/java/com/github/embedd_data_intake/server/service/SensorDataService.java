package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.PageDto;
import com.github.embedd_data_intake.server.dto.SensorDataDto;
import com.github.embedd_data_intake.server.dto.SensorDataEntryDto;
import com.github.embedd_data_intake.server.dto.SensorDataFilterDto;
import com.github.embedd_data_intake.server.model.SensorData;
import com.github.embedd_data_intake.server.repository.SensorDataRepository;
import com.github.embedd_data_intake.server.specification.SensorDataSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SensorDataService {
    private final SensorDataRepository sensorDataRepository;

    public SensorDataService(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }

    public SensorDataDto getDataByDevice(UUID deviceId, SensorDataFilterDto filter, Pageable pageable) {
        Specification<SensorData> spec = SensorDataSpecification.withFilter(deviceId, filter);

        Page<SensorDataEntryDto> readings = sensorDataRepository.findAll(spec, pageable)
                .map(SensorDataEntryDto::new);

        return new SensorDataDto(
                deviceId,
                readings.getContent(),
                new PageDto(
                        readings.getNumber(),
                        readings.getSize(),
                        readings.getNumberOfElements(),
                        readings.getTotalPages()
                )
        );
    }
}
