package com.github.embedd_data_intake.server.controller;

import com.github.embedd_data_intake.server.dto.AddDeviceDto;
import com.github.embedd_data_intake.server.dto.ShareRequestDto;
import com.github.embedd_data_intake.server.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/device")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
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
