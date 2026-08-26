package com.github.embedd_data_intake.server.controller;

import com.github.embedd_data_intake.server.dto.AuthRequestDto;
import com.github.embedd_data_intake.server.dto.AuthTokensDto;
import com.github.embedd_data_intake.server.dto.RefreshRequestDto;
import com.github.embedd_data_intake.server.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody AuthRequestDto request) {
        authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokensDto> login(@RequestBody AuthRequestDto request) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokensDto> refresh(@RequestBody RefreshRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /**
     * Single device logout: Invalidates provided refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequestDto request) {
        // TODO: Logout by refresh token id
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Global logout: Requires valid Bearer JWT and invalidates all session tokens for the user.
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UUID userId) {
        authService.logoutAll(userId);
        return ResponseEntity.noContent().build();
    }
}
