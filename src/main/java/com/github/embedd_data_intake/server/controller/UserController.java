package com.github.embedd_data_intake.server.controller;

import com.github.embedd_data_intake.server.annotation.ApplyAuth;
import com.github.embedd_data_intake.server.dto.*;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.service.AttributeService;
import com.github.embedd_data_intake.server.service.SensorDataService;
import com.github.embedd_data_intake.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@ApplyAuth
public class UserController {
    private final UserService userService;
    private final AttributeService attributeService;
    private final SensorDataService sensorDataService;

    public UserController(UserService userService, AttributeService attributeService, SensorDataService sensorDataService) {
        this.userService = userService;
        this.attributeService = attributeService;
        this.sensorDataService = sensorDataService;
    }

    @GetMapping
    public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping("/attributes")
    public ResponseEntity<List<AttributeTypeDto>> getUserAttributes(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(attributeService.getUserAttributes(userId));
    }

    @GetMapping("/device")
    public ResponseEntity<List<DeviceRoleDto>> getDevices(@AuthenticationPrincipal UUID userId, @RequestParam(name = "role", required = false) DeviceRole role) {
        return ResponseEntity.ok(userService.getUserDevices(userId, role));
    }

    @GetMapping("/data")
    public ResponseEntity<?> getUserTelemetry(
            @AuthenticationPrincipal UUID userId,
            @Valid SensorDataFilterDto filter,
            @PageableDefault(size = 20, sort = "id.timestamp", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        return ResponseEntity.ok(sensorDataService.getDataByUser(userId, filter, pageable));
    }
}
