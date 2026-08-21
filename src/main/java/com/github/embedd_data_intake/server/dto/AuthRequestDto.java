package com.github.embedd_data_intake.server.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AuthRequestDto {
    private String email;
    private String password;
}
