package com.github.embedd_data_intake.server.controller;

import com.github.embedd_data_intake.server.dto.ShareRequestDto;
import com.github.embedd_data_intake.server.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/device")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
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
