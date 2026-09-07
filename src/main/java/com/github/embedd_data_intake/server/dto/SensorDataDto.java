package com.github.embedd_data_intake.server.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SensorDataDto {
    private UUID deviceId;
    private List<SensorDataEntryDto> readings;
    private PageDto page;
}
