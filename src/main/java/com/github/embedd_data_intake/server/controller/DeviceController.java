package com.github.embedd_data_intake.server.controller;

import com.github.embedd_data_intake.server.annotation.ApplyAuth;
import com.github.embedd_data_intake.server.dto.*;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.service.AttributeService;
import com.github.embedd_data_intake.server.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/device")
@ApplyAuth
public class DeviceController {
    private static final String READ_PERMISSION = "@deviceSecurity.hasPermission(#deviceId, 'READ')";

    private final DeviceService deviceService;
    private final AttributeService attributeService;

    public DeviceController(DeviceService deviceService, AttributeService attributeService) {
        this.deviceService = deviceService;
        this.attributeService = attributeService;
    }

    @PostMapping
    public ResponseEntity<?> addDevice(@AuthenticationPrincipal UUID ownerId, @RequestBody AddDeviceDto device) {
        UUID deviceId = deviceService.addDevice(ownerId, device.getDeviceName());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{deviceId}")
                .buildAndExpand(deviceId)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{deviceId}")
    @PreAuthorize("@deviceSecurity.hasPermission(#deviceId, 'READ')")
    public ResponseEntity<DeviceDto> getDevice(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(deviceService.getDevice(deviceId));
    }

    @GetMapping("/{deviceId}/attributes")
    @PreAuthorize(READ_PERMISSION)
    public ResponseEntity<List<AttributeTypeDto>> getDeviceAttributes(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(attributeService.getDeviceAttributes(deviceId));
    }

    @GetMapping("/{deviceId}/access")
    @PreAuthorize("@deviceSecurity.hasPermission(#deviceId, 'ADMIN')")
    public ResponseEntity<List<UserRoleDto>> getDeviceAccess(@PathVariable UUID deviceId, @RequestParam(name = "role", required = false) DeviceRole role) {
        return ResponseEntity.ok(deviceService.getDeviceAccess(deviceId, role));
    }

    @GetMapping("{deviceId}/telemetry")
    @PreAuthorize("@deviceSecurity.hasPermission(#deviceId, 'READ')")
    public ResponseEntity<?> getDeviceTelemetry(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(deviceService.getTelemetry(deviceId));
    }

    @PostMapping("/{deviceId}/share")
    @PreAuthorize("@deviceSecurity.hasPermission(#deviceId, 'ADMIN')")
    public ResponseEntity<Void> shareDevice(
            @PathVariable UUID deviceId,
            @RequestBody ShareRequestDto request) {
        deviceService.grantAccess(deviceId, request.getTargetUserEmail(), request.getRole());
        return ResponseEntity.ok().build();
    }
}
