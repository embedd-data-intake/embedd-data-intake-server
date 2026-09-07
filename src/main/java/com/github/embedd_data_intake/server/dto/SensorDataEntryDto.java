package com.github.embedd_data_intake.server.dto;

import com.github.embedd_data_intake.server.enums.AttributeType;
import com.github.embedd_data_intake.server.model.SensorData;
import lombok.*;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SensorDataEntryDto {
    private OffsetDateTime timestamp;
    private String attribute;
    private AttributeType type;
    private Object value;

    public SensorDataEntryDto(SensorData sensorData) {
        this.timestamp = sensorData.getId().getTimestamp();
        this.attribute = sensorData.getAttribute().getAttributeName();
        this.type = sensorData.getAttribute().getType();
        this.value = sensorData.getVal(this.type);
    }
}
