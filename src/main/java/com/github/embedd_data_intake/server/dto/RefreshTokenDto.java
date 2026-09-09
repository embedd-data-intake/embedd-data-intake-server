package com.github.embedd_data_intake.server.dto;

import com.github.embedd_data_intake.server.model.RefreshToken;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RefreshTokenDto {
    private UUID id;
    private UUID token;

    public RefreshTokenDto(RefreshToken refreshToken) {
        this.id = refreshToken.getId();
        this.token = refreshToken.getToken();
    }
}
