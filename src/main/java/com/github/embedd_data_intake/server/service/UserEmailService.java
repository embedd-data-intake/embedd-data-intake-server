package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.model.UserEmail;
import com.github.embedd_data_intake.server.repository.UserEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserEmailService {
    private final UserEmailRepository userEmailRepository;

    public UserEmailService(UserEmailRepository userEmailRepository) {
        this.userEmailRepository = userEmailRepository;
    }

    @Transactional
    public void linkEmailToUser(UUID emailId, UUID userId) {
        // TODO: Add email confirmation
        UserEmail userEmail = new UserEmail();
        userEmail.setUserId(userId);
        userEmail.setEmailId(emailId);
        userEmailRepository.save(userEmail);
    }
}
