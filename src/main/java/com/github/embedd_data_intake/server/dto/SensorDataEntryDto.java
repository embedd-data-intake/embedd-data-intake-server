package com.github.embedd_data_intake.server.dto;

import lombok.*;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SensorDataEntryDto {
    private OffsetDateTime timestamp;
    private Object value;
}
