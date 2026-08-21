package com.github.embedd_data_intake.server.dto;

import lombok.*;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ExceptionDetailsDto {
    private OffsetDateTime dateTime;
    private String message;
    private String details;
}
