package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.DeviceRoleDto;
import com.github.embedd_data_intake.server.dto.EmailDto;
import com.github.embedd_data_intake.server.dto.UserDeviceDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.ConflictException;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import com.github.embedd_data_intake.server.model.User;
import com.github.embedd_data_intake.server.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    private final EmailService emailService;
    private final UserDeviceService userDeviceService;
    private final UserEmailService userEmailService;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, EmailService emailService, UserDeviceService userDeviceService, UserEmailService userEmailService, PasswordEncoder passwordEncoder) {
        this.emailService = emailService;
        this.userDeviceService = userDeviceService;
        this.userRepository = userRepository;
        this.userEmailService = userEmailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @param userId for which to find the devices
     * @param role (optional) to filter devices by roles
     * @return list of device ids and the role the user has with them
     */
    public List<DeviceRoleDto> getUserDevices(UUID userId, DeviceRole role) {
        List<UserDeviceDto> userDevices;
        if (Objects.isNull(role)) {
            userDevices = userDeviceService.findByUserId(userId);
        } else {
            userDevices = userDeviceService.findByUserIdAndRole(userId, role);
        }

        return userDevices.stream().map(DeviceRoleDto::new).toList();
    }

    public UserDto getUser(UUID userId) {
        EmailDto emailDto = emailService.getEmailByUserId(userId);

        return new UserDto(userId, emailDto.getEmailAddress());
    }

    public UserDto getUser(String emailAddress) {
        User user = userRepository.findByUserEmails_Email_EmailAddress(emailAddress)
                .orElseThrow(() -> new NotFoundException("Email for user not found."));

        return new UserDto(user.getId(), emailAddress);
    }

    public UserDto getUser(String emailAddress, String rawPassword) {
        User user = userRepository.findByUserEmails_Email_EmailAddress(emailAddress)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        return new UserDto(user.getId(), emailAddress);
    }

    @Transactional
    public UserDto createUser(String emailAddress, String passwordHash) throws ConflictException {
        if (emailService.isEmailInUse(emailAddress)) {
            throw new ConflictException("Email address is already in use.");
        }

        User user = new User();
        user.setPasswordHash(passwordHash);
        userRepository.save(user);

        EmailDto newEmail = emailService.addOrGetEmail(emailAddress);

        userEmailService.linkEmailToUser(newEmail.getId(), user.getId());

        return new UserDto(user.getId(), emailAddress);
    }

    public UserDto getUserByRefreshToken(UUID token) throws UnauthorizedException {
        User user = userRepository.findByRefreshTokens_Token(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token expired or invalid. Please sign in."));

        EmailDto email = emailService.getEmailByUserId(user.getId());

        return new UserDto(user.getId(), email.getEmailAddress());
    }
}
