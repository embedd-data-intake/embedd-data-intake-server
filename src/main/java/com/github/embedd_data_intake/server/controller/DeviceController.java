package com.github.embedd_data_intake.server.controller;

import com.github.embedd_data_intake.server.annotation.ApplyAuth;
import com.github.embedd_data_intake.server.dto.*;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.service.AttributeService;
import com.github.embedd_data_intake.server.service.DeviceService;
import com.github.embedd_data_intake.server.service.SensorDataService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    private static final String ADMIN_PERMISSION = "@deviceSecurity.hasPermission(#deviceId, 'ADMIN')";

    private final DeviceService deviceService;
    private final AttributeService attributeService;
    private final SensorDataService sensorDataService;

    public DeviceController(DeviceService deviceService, AttributeService attributeService, SensorDataService sensorDataService) {
        this.deviceService = deviceService;
        this.attributeService = attributeService;
        this.sensorDataService = sensorDataService;
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
    @PreAuthorize(READ_PERMISSION)
    public ResponseEntity<DeviceDto> getDevice(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(deviceService.getDevice(deviceId));
    }

    @GetMapping("/{deviceId}/attributes")
    @PreAuthorize(READ_PERMISSION)
    public ResponseEntity<List<AttributeTypeDto>> getDeviceAttributes(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(attributeService.getDeviceAttributes(deviceId));
    }

    @GetMapping("/{deviceId}/access")
    @PreAuthorize(ADMIN_PERMISSION)
    public ResponseEntity<List<UserRoleDto>> getDeviceAccess(@PathVariable UUID deviceId, @RequestParam(name = "role", required = false) DeviceRole role) {
        return ResponseEntity.ok(deviceService.getDeviceAccess(deviceId, role));
    }

    @GetMapping("{deviceId}/data")
    @PreAuthorize(READ_PERMISSION)
    public ResponseEntity<SensorDataDto> getDeviceTelemetry(
            @PathVariable UUID deviceId,
            @Valid SensorDataFilterDto filter,
            @PageableDefault(size = 20, sort = "id.atimestamp", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        return ResponseEntity.ok(sensorDataService.getDataByDevice(deviceId, filter, pageable));
    }

    @PostMapping("/{deviceId}/share")
    @PreAuthorize(ADMIN_PERMISSION)
    public ResponseEntity<Void> shareDevice(
            @PathVariable UUID deviceId,
            @RequestBody ShareRequestDto request) {
        deviceService.grantAccess(deviceId, request.getTargetUserEmail(), request.getRole());
        return ResponseEntity.ok().build();
    }
}
