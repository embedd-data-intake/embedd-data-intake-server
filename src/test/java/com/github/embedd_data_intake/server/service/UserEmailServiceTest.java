package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.model.UserEmail;
import com.github.embedd_data_intake.server.repository.UserEmailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEmailServiceTest {

    @Mock
    private UserEmailRepository userEmailRepository;

    @InjectMocks
    private UserEmailService userEmailService;

    @Nested
    @DisplayName("linkEmailToUser()")
    class LinkEmailToUserTests {
        @Test
        void linkEmailToUser_SavesUserEmailWithCorrectFields() {
            UUID emailId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            userEmailService.linkEmailToUser(emailId, userId);

            ArgumentCaptor<UserEmail> userEmailCaptor = ArgumentCaptor.forClass(UserEmail.class);
            verify(userEmailRepository).save(userEmailCaptor.capture());

            UserEmail savedUserEmail = userEmailCaptor.getValue();
            assertThat(savedUserEmail).isNotNull();
            assertThat(savedUserEmail.getEmailId()).isEqualTo(emailId);
            assertThat(savedUserEmail.getUserId()).isEqualTo(userId);
        }
    }
}