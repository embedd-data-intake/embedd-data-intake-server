package com.github.embedd_data_intake.server.dto;

import com.github.embedd_data_intake.server.model.Email;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EmailDto {
    private UUID id;
    private String emailAddress;

    public EmailDto(Email email) {
        this.id = email.getId();
        this.emailAddress = email.getEmailAddress();
    }
}
