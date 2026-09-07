package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.*;
import com.github.embedd_data_intake.server.enums.AttributeType;
import com.github.embedd_data_intake.server.model.Attribute;
import com.github.embedd_data_intake.server.model.SensorData;
import com.github.embedd_data_intake.server.repository.SensorDataRepository;
import com.github.embedd_data_intake.server.specification.SensorDataSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SensorDataService {
    private final SensorDataRepository sensorDataRepository;

    public SensorDataService(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }

    public SensorDataDto getDataByDevice(UUID deviceId, SensorDataFilterDto filter, Pageable pageable) {
        Page<SensorData> pageResult = sensorDataRepository.findAll(
                SensorDataSpecification.withFilter(deviceId, filter),
                pageable
        );

        List<SensorDataCollectionDto> groupedReadings = groupSensorData(pageResult.getContent());

        return new SensorDataDto(
                deviceId,
                groupedReadings,
                new PageDto(pageResult)
        );
    }

    // Private helper to isolate the stream grouping logic
    private List<SensorDataCollectionDto> groupSensorData(List<SensorData> dataList) {
        Map<Attribute, List<SensorDataEntryDto>> groupedMap = dataList.stream()
                .collect(Collectors.groupingBy(
                        SensorData::getAttribute,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toSensorDataEntryDto, Collectors.toList())
                ));

        return groupedMap.entrySet().stream()
                .map(entry -> new SensorDataCollectionDto(
                        entry.getKey().getAttributeName(),
                        entry.getKey().getType(),
                        entry.getValue()
                ))
                .toList();
    }

    // Dedicated mapper method for individual entries
    private SensorDataEntryDto toSensorDataEntryDto(SensorData data) {
        AttributeType type = data.getAttribute().getType();
        return new SensorDataEntryDto(
                data.getId().getTimestamp(),
                data.getVal(type)
        );
    }
}
