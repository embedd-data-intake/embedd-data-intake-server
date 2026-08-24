package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.AuthTokensDto;
import com.github.embedd_data_intake.server.exceptions.ConflictException;
import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import com.github.embedd_data_intake.server.model.Email;
import com.github.embedd_data_intake.server.model.RefreshToken;
import com.github.embedd_data_intake.server.model.User;
import com.github.embedd_data_intake.server.model.UserEmail;
import com.github.embedd_data_intake.server.repository.EmailRepository;
import com.github.embedd_data_intake.server.repository.UserEmailRepository;
import com.github.embedd_data_intake.server.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final EmailRepository emailRepository;
    private final UserEmailRepository userEmailRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            EmailRepository emailRepository,
            UserEmailRepository userEmailRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.emailRepository = emailRepository;
        this.userEmailRepository = userEmailRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void register(String rawEmailAddress, String rawPassword) {
        String emailAddress = rawEmailAddress.trim().toLowerCase();

        if (userEmailRepository.existsByEmail_EmailAddress(emailAddress)) {
            throw new ConflictException("Email address is already in use.");
        }

        User user = new User();
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(user);

        Email email = emailRepository.findByEmailAddress(emailAddress)
                .orElseGet(() -> {
                    Email newEmail = new Email();
                    newEmail.setEmailAddress(emailAddress);
                    return emailRepository.save(newEmail);
                });

        UserEmail userEmail = new UserEmail();
        userEmail.setUser(user);
        userEmail.setEmail(email);
        userEmailRepository.save(userEmail);
    }

    @Transactional
    public AuthTokensDto login(String rawEmailAddress, String rawPassword) {
        String emailAddress = rawEmailAddress.trim().toLowerCase();

        User user = userRepository.findByUserEmails_Email_EmailAddress(emailAddress)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String accessToken = jwtService.generateAccessToken(user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new AuthTokensDto(accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthTokensDto refresh(UUID refreshToken) {
        RefreshToken tokenRecord = refreshTokenService.verifyExpiration(refreshToken);
        User user = tokenRecord.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getId());

        return new AuthTokensDto(newAccessToken, refreshToken);
    }

    @Transactional
    public void logout(UUID refreshToken) {
        refreshTokenService.invalidateToken(refreshToken);
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenService.invalidateAllTokens(userId);
    }
}
