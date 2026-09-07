package com.github.embedd_data_intake.server.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SensorDataFilterDto {
    OffsetDateTime from;
    OffsetDateTime to;
    List<String> attributes;
}
