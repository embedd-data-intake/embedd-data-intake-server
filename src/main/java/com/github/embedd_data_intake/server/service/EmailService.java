package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.EmailDto;
import com.github.embedd_data_intake.server.dto.UserRoleDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.model.Email;
import com.github.embedd_data_intake.server.repository.EmailRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmailService {
    private final EmailRepository emailRepository;

    public EmailService(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    public List<UserRoleDto> getAccessForDevice(UUID deviceId) {
        return emailRepository.findActiveUserEmailsForDevice(deviceId);
    }

    public List<UserRoleDto> getAccessForDevice(UUID deviceId, DeviceRole role) {
        return emailRepository.findActiveUserEmailsForDeviceAndRole(deviceId, role);
    }

    public EmailDto getEmailByUserId(UUID userId) {
        Email email = emailRepository.findByUserEmails_UserId(userId)
                .orElseThrow(() -> new NotFoundException("Email for user not found."));

        return new EmailDto(email);
    }
}
