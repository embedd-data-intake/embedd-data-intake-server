package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.DeviceRoleDto;
import com.github.embedd_data_intake.server.dto.EmailDto;
import com.github.embedd_data_intake.server.dto.UserDeviceDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.impl.ConflictException;
import com.github.embedd_data_intake.server.exceptions.impl.NotFoundException;
import com.github.embedd_data_intake.server.exceptions.impl.UnauthorizedException;
import com.github.embedd_data_intake.server.model.User;
import com.github.embedd_data_intake.server.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private UserDeviceService userDeviceService;

    @Mock
    private UserEmailService userEmailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("getUserDevices()")
    class GetUserDevicesTests {
        @Test
        void getUserDevices_WhenRoleIsNull_CallsFindByUserId() {
            UUID userId = UUID.randomUUID();
            UserDeviceDto mockDevice = new UserDeviceDto(); // assumes no-arg or standard DTO
            given(userDeviceService.findByUserId(userId)).willReturn(List.of(mockDevice));

            List<DeviceRoleDto> result = userService.getUserDevices(userId, null);

            assertThat(result).hasSize(1);
            verify(userDeviceService).findByUserId(userId);
            verify(userDeviceService, never()).findByUserIdAndRole(any(), any());
        }

        @Test
        void getUserDevices_WhenRoleProvided_CallsFindByUserIdAndRole() {
            UUID userId = UUID.randomUUID();
            DeviceRole role = DeviceRole.ADMIN;
            UserDeviceDto mockDevice = new UserDeviceDto();
            given(userDeviceService.findByUserIdAndRole(userId, role)).willReturn(List.of(mockDevice));

            List<DeviceRoleDto> result = userService.getUserDevices(userId, role);

            assertThat(result).hasSize(1);
            verify(userDeviceService).findByUserIdAndRole(userId, role);
            verify(userDeviceService, never()).findByUserId(userId);
        }
    }

    @Nested
    @DisplayName("getUser(userId)")
    class GetUserByUserIdTests {
        @Test
        void getUser_WhenUserIdExists_ReturnsUserDto() {
            UUID userId = UUID.randomUUID();
            String email = "user@example.com";
            EmailDto emailDto = new EmailDto(UUID.randomUUID(), email);

            given(emailService.getEmailByUserId(userId)).willReturn(emailDto);

            UserDto result = userService.getUser(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmailAddress()).isEqualTo(email);
            verify(emailService).getEmailByUserId(userId);
        }

        @Test
        void getUser_WhenUserIdNotFound_PropagatesException() {
            UUID userId = UUID.randomUUID();
            given(emailService.getEmailByUserId(userId))
                    .willThrow(new NotFoundException("User email not found."));

            assertThatThrownBy(() -> userService.getUser(userId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User email not found.");
        }
    }

    @Nested
    @DisplayName("getUser(emailAddress)")
    class GetUserByEmailAddressTests {
        @Test
        void getUser_WhenEmailExists_ReturnsUserDto() {
            String email = "existing@example.com";
            UUID userId = UUID.randomUUID();
            User mockUser = new User();
            mockUser.setId(userId);

            given(userRepository.findByUserEmails_Email_EmailAddress(email))
                    .willReturn(Optional.of(mockUser));

            UserDto result = userService.getUser(email);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmailAddress()).isEqualTo(email);
        }

        @Test
        void getUser_WhenEmailNotFound_ThrowsNotFoundException() {
            String email = "missing@example.com";
            given(userRepository.findByUserEmails_Email_EmailAddress(email))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(email))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Email for user not found.");
        }
    }

    @Nested
    @DisplayName("getUser(email, password)")
    class GetUserWithPasswordTests {
        @Test
        void getUser_WithValidCredentials_ReturnsUserDto() {
            String email = "user@example.com";
            String rawPassword = "secretPassword";
            String encodedHash = "$2a$10$encodedHashValue";
            UUID userId = UUID.randomUUID();

            User mockUser = new User();
            mockUser.setId(userId);
            mockUser.setPasswordHash(encodedHash);

            given(userRepository.findByUserEmails_Email_EmailAddress(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(rawPassword, encodedHash)).willReturn(true);

            UserDto result = userService.getUser(email, rawPassword);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmailAddress()).isEqualTo(email);
        }

        @Test
        void getUser_WithUnknownEmail_ThrowsUnauthorizedException() {
            String email = "unknown@example.com";
            given(userRepository.findByUserEmails_Email_EmailAddress(email)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(email, "anyPassword"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password.");
        }

        @Test
        void getUser_WithWrongPassword_ThrowsUnauthorizedException() {
            String email = "user@example.com";
            User mockUser = new User();
            mockUser.setPasswordHash("hashedPassword");

            given(userRepository.findByUserEmails_Email_EmailAddress(email)).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches("wrongPass", "hashedPassword")).willReturn(false);

            assertThatThrownBy(() -> userService.getUser(email, "wrongPass"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password.");
        }
    }

    @Nested
    @DisplayName("createUser()")
    class CreateUserTests {

        @Test
        void createUser_WhenEmailNotInUse_CreatesUserAndLinksEmail() throws ConflictException {
            String email = "newuser@example.com";
            String passwordHash = "hashedPassword";
            UUID emailId = UUID.randomUUID();

            EmailDto createdEmailDto = new EmailDto(emailId, email);

            given(emailService.isEmailInUse(email)).willReturn(false);
            given(emailService.addOrGetEmail(email)).willReturn(createdEmailDto);

            UserDto result = userService.createUser(email, passwordHash);

            assertThat(result).isNotNull();
            assertThat(result.getEmailAddress()).isEqualTo(email);

            verify(userRepository).save(any(User.class));
            verify(emailService).addOrGetEmail(email);
            verify(userEmailService).linkEmailToUser(eq(emailId), any());
        }

        @Test
        void createUser_WhenEmailAlreadyInUse_ThrowsConflictException() {
            String email = "existing@example.com";
            given(emailService.isEmailInUse(email)).willReturn(true);

            assertThatThrownBy(() -> userService.createUser(email, "passwordHash"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Email address is already in use.");

            verify(userRepository, never()).save(any());
            verify(userEmailService, never()).linkEmailToUser(any(), any());
        }
    }

    @Nested
    @DisplayName("getUserByRefreshToken()")
    class GetUserByRefreshTokenTests {
        @Test
        void getUserByRefreshToken_WhenTokenValid_ReturnsUserDto() {
            UUID refreshToken = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String email = "tokenuser@example.com";

            User mockUser = new User();
            mockUser.setId(userId);

            given(userRepository.findByRefreshTokens_Token(refreshToken)).willReturn(Optional.of(mockUser));
            given(emailService.getEmailByUserId(userId)).willReturn(new EmailDto(UUID.randomUUID(), email));

            UserDto result = userService.getUserByRefreshToken(refreshToken);

            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmailAddress()).isEqualTo(email);
        }

        @Test
        void getUserByRefreshToken_WhenTokenInvalid_ThrowsUnauthorizedException() {
            UUID refreshToken = UUID.randomUUID();
            given(userRepository.findByRefreshTokens_Token(refreshToken)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByRefreshToken(refreshToken))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token expired or invalid. Please sign in.");
        }
    }
}