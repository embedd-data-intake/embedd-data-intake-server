package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.EmailDto;
import com.github.embedd_data_intake.server.dto.UserRoleDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.model.Email;
import com.github.embedd_data_intake.server.repository.EmailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

    @InjectMocks
    private EmailService emailService;

    // Helper method to create real Email entities without setters for ID
    private Email createEmail(UUID id, String emailAddress) {
        Email email = new Email();
        ReflectionTestUtils.setField(email, "id", id);
        email.setEmailAddress(emailAddress);
        return email;
    }

    @Nested
    @DisplayName("getAccessForDevice()")
    class GetAccessForDeviceTests {
        @Test
        void getAccessForDevice_WithoutRole_CallsFindActiveUserEmailsForDevice() {
            UUID deviceId = UUID.randomUUID();
            UserRoleDto mockUserRole = mock(UserRoleDto.class);

            given(emailRepository.findActiveUserEmailsForDevice(deviceId))
                    .willReturn(List.of(mockUserRole));

            List<UserRoleDto> result = emailService.getAccessForDevice(deviceId);

            assertThat(result).hasSize(1);
            verify(emailRepository).findActiveUserEmailsForDevice(deviceId);
            verify(emailRepository, never()).findActiveUserEmailsForDeviceAndRole(any(), any());
        }

        @Test
        void getAccessForDevice_WithRole_CallsFindActiveUserEmailsForDeviceAndRole() {
            UUID deviceId = UUID.randomUUID();
            DeviceRole role = DeviceRole.OWNER;
            UserRoleDto mockUserRole = mock(UserRoleDto.class);

            given(emailRepository.findActiveUserEmailsForDeviceAndRole(deviceId, role))
                    .willReturn(List.of(mockUserRole));

            List<UserRoleDto> result = emailService.getAccessForDevice(deviceId, role);

            assertThat(result).hasSize(1);
            verify(emailRepository).findActiveUserEmailsForDeviceAndRole(deviceId, role);
            verify(emailRepository, never()).findActiveUserEmailsForDevice(any());
        }
    }

    @Nested
    @DisplayName("getEmailByUserId()")
    class GetEmailByUserIdTests {
        @Test
        void getEmailByUserId_WhenEmailExists_ReturnsEmailDto() {
            UUID userId = UUID.randomUUID();
            UUID emailId = UUID.randomUUID();
            String address = "user@example.com";
            Email mockEmail = createEmail(emailId, address);

            given(emailRepository.findByUserEmails_UserId(userId)).willReturn(Optional.of(mockEmail));

            EmailDto result = emailService.getEmailByUserId(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(emailId);
            assertThat(result.getEmailAddress()).isEqualTo(address);

            verify(emailRepository).findByUserEmails_UserId(userId);
        }

        @Test
        void getEmailByUserId_WhenNotFound_ThrowsNotFoundException() {
            UUID userId = UUID.randomUUID();
            given(emailRepository.findByUserEmails_UserId(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> emailService.getEmailByUserId(userId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Email for user not found.");
        }
    }

    @Nested
    @DisplayName("isEmailInUse()")
    class IsEmailInUseTests {

        @Test
        void isEmailInUse_WhenEmailExists_ReturnsTrue() {
            String address = "used@example.com";
            given(emailRepository.existsByEmailAddress(address)).willReturn(true);

            boolean inUse = emailService.isEmailInUse(address);

            assertThat(inUse).isTrue();
            verify(emailRepository).existsByEmailAddress(address);
        }

        @Test
        void isEmailInUse_WhenEmailDoesNotExist_ReturnsFalse() {
            String address = "free@example.com";
            given(emailRepository.existsByEmailAddress(address)).willReturn(false);

            boolean inUse = emailService.isEmailInUse(address);

            assertThat(inUse).isFalse();
            verify(emailRepository).existsByEmailAddress(address);
        }
    }

    @Nested
    @DisplayName("addOrGetEmail()")
    class AddOrGetEmailTests {

        @Test
        void addOrGetEmail_WhenEmailAlreadyExists_ReturnsExistingEmail() {
            String address = "existing@example.com";
            UUID emailId = UUID.randomUUID();
            Email existingEmail = createEmail(emailId, address);

            given(emailRepository.findByEmailAddress(address)).willReturn(Optional.of(existingEmail));

            EmailDto result = emailService.addOrGetEmail(address);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(emailId);
            assertThat(result.getEmailAddress()).isEqualTo(address);

            verify(emailRepository, never()).save(any());
        }

        @Test
        void addOrGetEmail_WhenEmailDoesNotExist_SavesAndReturnsNewEmail() {
            String address = "new@example.com";
            UUID generatedId = UUID.randomUUID();

            given(emailRepository.findByEmailAddress(address)).willReturn(Optional.empty());

            doAnswer(invocation -> {
                Email emailToSave = invocation.getArgument(0);
                ReflectionTestUtils.setField(emailToSave, "id", generatedId);
                return emailToSave;
            }).when(emailRepository).save(any(Email.class));

            EmailDto result = emailService.addOrGetEmail(address);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(generatedId);
            assertThat(result.getEmailAddress()).isEqualTo(address);

            verify(emailRepository).findByEmailAddress(address);
            verify(emailRepository).save(argThat(email -> address.equals(email.getEmailAddress())));
        }
    }
}