package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.AuthTokensDto;
import com.github.embedd_data_intake.server.exceptions.UnauthorizedException;
import com.github.embedd_data_intake.server.model.Email;
import com.github.embedd_data_intake.server.model.RefreshToken;
import com.github.embedd_data_intake.server.model.User;
import com.github.embedd_data_intake.server.model.UserEmail;
import com.github.embedd_data_intake.server.repository.EmailRepository;
import com.github.embedd_data_intake.server.repository.RefreshTokenRepository;
import com.github.embedd_data_intake.server.repository.UserEmailRepository;
import com.github.embedd_data_intake.server.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final EmailRepository emailRepository;
    private final UserEmailRepository userEmailRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, EmailRepository emailRepository, UserEmailRepository userEmailRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.emailRepository = emailRepository;
        this.userEmailRepository = userEmailRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void register(String rawEmailAddress, String rawPassword) {
        String emailAddress = rawEmailAddress.trim().toLowerCase();

        if (userEmailRepository.existsByEmail_EmailAddress(emailAddress)) {
            // Throw conflict
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
    public AuthTokensDto refresh(UUID refreshTokenValue) {
        RefreshToken tokenRecord = refreshTokenService.verifyExpiration(refreshTokenValue);

        User user = tokenRecord.getUser();

        // Fetch active email for token claims
        UserEmail activeUserEmail = user.getUserEmails().stream()
                .filter(ue -> ue.getDeletedAt() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User has no active email"));

        String newAccessToken = jwtService.generateAccessToken(user.getId());

        return new AuthTokensDto(newAccessToken, refreshTokenValue);
    }
}
