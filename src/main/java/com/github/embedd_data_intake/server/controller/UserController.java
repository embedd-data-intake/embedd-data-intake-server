package com.github.embedd_data_intake.server.controller;

import com.github.embedd_data_intake.server.annotation.ApplyAuth;
import com.github.embedd_data_intake.server.dto.DeviceRoleDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@ApplyAuth
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping("/device")
    public ResponseEntity<List<DeviceRoleDto>> getDevices(@AuthenticationPrincipal UUID userId, @RequestParam(name = "role", required = false) DeviceRole role) {
        return ResponseEntity.ok(userService.getUserDevices(userId, role));
    }
}
