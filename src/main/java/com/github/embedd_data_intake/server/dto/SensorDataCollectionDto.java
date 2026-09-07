package com.github.embedd_data_intake.server.dto;

import com.github.embedd_data_intake.server.enums.AttributeType;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SensorDataCollectionDto {
    private String attribute;
    private AttributeType type;
    private List<SensorDataEntryDto> entries;
}
