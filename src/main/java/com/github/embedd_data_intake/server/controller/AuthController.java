package com.github.embedd_data_intake.server.controller;

import com.github.embedd_data_intake.server.annotation.ApplyAuth;
import com.github.embedd_data_intake.server.dto.AuthRequestDto;
import com.github.embedd_data_intake.server.dto.AuthTokensDto;
import com.github.embedd_data_intake.server.dto.RefreshRequestDto;
import com.github.embedd_data_intake.server.service.AuthService;
import com.github.embedd_data_intake.server.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
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
    @ApplyAuth
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        UUID sid = jwtService.extractSessionId(token);

        authService.logout(sid);

        return ResponseEntity.noContent().build();
    }

    /**
     * Global logout: Requires valid Bearer JWT and invalidates all session tokens for the user.
     */
    @PostMapping("/logout-all")
    @ApplyAuth
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UUID userId) {
        authService.logoutAll(userId);
        return ResponseEntity.noContent().build();
    }
}
