package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.*;
import com.github.embedd_data_intake.server.enums.AttributeType;
import com.github.embedd_data_intake.server.model.Attribute;
import com.github.embedd_data_intake.server.model.SensorData;
import com.github.embedd_data_intake.server.model.SensorDataId;
import com.github.embedd_data_intake.server.repository.SensorDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorDataServiceTest {
    @Mock
    private SensorDataRepository sensorDataRepository;

    @InjectMocks
    private SensorDataService sensorDataService;

    // Helper method to create real Attribute entities
    private Attribute createAttribute(UUID id, String name, AttributeType type) {
        Attribute attribute = new Attribute();
        ReflectionTestUtils.setField(attribute, "id", id);
        ReflectionTestUtils.setField(attribute, "attributeName", name);
        ReflectionTestUtils.setField(attribute, "type", type);
        return attribute;
    }

    // Helper method to set up real or stubbed SensorData
    private SensorData createSensorData(UUID deviceId, OffsetDateTime timestamp, Attribute attribute, Object val) {
        SensorDataId id = new SensorDataId(timestamp, deviceId, attribute.getId());

        // We can use Mockito on SensorData only for dynamic getVal(type) dispatch,
        // or populate real fields if SensorData allows field setting.
        SensorData sensorData = mock(SensorData.class);
        given(sensorData.getId()).willReturn(id);
        given(sensorData.getAttribute()).willReturn(attribute);
        given(sensorData.getVal(attribute.getType())).willReturn(val);

        return sensorData;
    }

    @Nested
    @DisplayName("getDataByDevice()")
    class GetDataByDeviceTests {
        @Test
        void getDataByDevice_WhenDataExists_GroupsByRealAttributeAndReturnsDto() {
            UUID deviceId = UUID.randomUUID();
            SensorDataFilterDto filter = new SensorDataFilterDto();
            Pageable pageable = PageRequest.of(0, 10);

            Attribute tempAttr = createAttribute(UUID.randomUUID(), "temperature", AttributeType.NUMBER);

            OffsetDateTime now = OffsetDateTime.now();
            SensorData data1 = createSensorData(deviceId, now, tempAttr, 23.5f);
            SensorData data2 = createSensorData(deviceId, now.plusSeconds(60), tempAttr, 24.0f);

            List<SensorData> content = List.of(data1, data2);
            Page<SensorData> mockPage = new PageImpl<>(content, pageable, content.size());

            given(sensorDataRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .willReturn(mockPage);

            // Act
            SensorDataDto result = sensorDataService.getDataByDevice(deviceId, filter, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getDeviceId()).isEqualTo(deviceId);

            List<SensorDataCollectionDto> collections = result.getReadings();
            assertThat(collections).hasSize(1);

            SensorDataCollectionDto tempCollection = collections.getFirst();
            assertThat(tempCollection.getAttribute()).isEqualTo("temperature");
            assertThat(tempCollection.getType()).isEqualTo(AttributeType.NUMBER);
            assertThat(tempCollection.getEntries()).hasSize(2);

            verify(sensorDataRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        void getDataByDevice_WhenNoData_ReturnsEmptyReadings() {
            UUID deviceId = UUID.randomUUID();
            SensorDataFilterDto filter = new SensorDataFilterDto();
            Pageable pageable = PageRequest.of(0, 10);

            Page<SensorData> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            given(sensorDataRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .willReturn(emptyPage);

            SensorDataDto result = sensorDataService.getDataByDevice(deviceId, filter, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getDeviceId()).isEqualTo(deviceId);
            assertThat(result.getReadings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDataByUser()")
    class GetDataByUserTests {
        @Test
        void getDataByUser_WhenDataExists_GroupsDataByDeviceId() {
            UUID userId = UUID.randomUUID();
            UUID deviceId1 = UUID.randomUUID();
            UUID deviceId2 = UUID.randomUUID();
            SensorDataFilterDto filter = new SensorDataFilterDto();
            Pageable pageable = PageRequest.of(0, 10);

            Attribute humidityAttr = createAttribute(UUID.randomUUID(), "humidity", AttributeType.NUMBER);

            OffsetDateTime now = OffsetDateTime.now();
            SensorData data1 = createSensorData(deviceId1, now, humidityAttr, 45);
            SensorData data2 = createSensorData(deviceId2, now, humidityAttr, 50);

            List<SensorData> content = List.of(data1, data2);
            Page<SensorData> pageResult = new PageImpl<>(content, pageable, content.size());

            given(sensorDataRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .willReturn(pageResult);

            Map<UUID, List<SensorDataCollectionDto>> result = sensorDataService.getDataByUser(userId, filter, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).containsKeys(deviceId1, deviceId2);
            assertThat(result.get(deviceId1)).isNotEmpty();
            assertThat(result.get(deviceId2)).isNotEmpty();

            verify(sensorDataRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        void getDataByUser_WhenNoData_ReturnsEmptyMap() {
            UUID userId = UUID.randomUUID();
            SensorDataFilterDto filter = new SensorDataFilterDto();
            Pageable pageable = PageRequest.of(0, 10);

            Page<SensorData> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            given(sensorDataRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .willReturn(emptyPage);

            Map<UUID, List<SensorDataCollectionDto>> result = sensorDataService.getDataByUser(userId, filter, pageable);

            assertThat(result).isEmpty();
        }
    }
}