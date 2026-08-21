package com.github.embedd_data_intake.server.dto;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RefreshRequestDto {
    private UUID refreshToken;
}
