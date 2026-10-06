package com.github.embedd_data_intake.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.embedd_data_intake.server.config.SecurityConfig;
import com.github.embedd_data_intake.server.dto.*;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.impl.NotFoundException;
import com.github.embedd_data_intake.server.handler.RestExceptionHandler;
import com.github.embedd_data_intake.server.security.DeviceSecurityService;
import com.github.embedd_data_intake.server.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@Import({RestExceptionHandler.class, SecurityConfig.class})
class DeviceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private DeviceService deviceService;

    @MockitoBean
    private AttributeService attributeService;

    @MockitoBean
    private SensorDataService sensorDataService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean(name = "deviceSecurity")
    private DeviceSecurityService deviceSecurity;

    private UUID ownerId;
    private UUID deviceId;
    private UUID sessionId;
    private String bearerHeader;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        deviceId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        bearerHeader = "Bearer " + UUID.randomUUID();

        authenticatedSetup();
    }

    void authenticatedSetup() {
        when(jwtService.extractSessionId(anyString())).thenReturn(sessionId);
        when(refreshTokenService.existsById(sessionId)).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(ownerId);
    }


    @Nested
    @DisplayName("POST /api/v1/device - addDevice")
    class AddDeviceTests {
        @Test
        @WithUserDetails
        @DisplayName("Should return 201 Created and set Location header")
        void addDevice_Success() throws Exception {
            AddDeviceDto addDeviceDto = new AddDeviceDto();
            addDeviceDto.setDeviceName("Smart Thermostat");

            given(deviceService.addDevice(eq(ownerId), eq("Smart Thermostat"))).willReturn(deviceId);

            mockMvc.perform(post("/api/v1/device")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                            .principal(() -> ownerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addDeviceDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/v1/device/" + deviceId)));

            verify(deviceService).addDevice(ownerId, "Smart Thermostat");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/device/{deviceId} - getDevice")
    class GetDeviceTests {
        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK with DeviceDto when device exists")
        void getDevice_Success() throws Exception {
            DeviceDto deviceDto = new DeviceDto();
            given(deviceService.getDevice(deviceId)).willReturn(deviceDto);
            when(deviceSecurity.hasPermission(deviceId, "READ")).thenReturn(true);

            mockMvc.perform(get("/api/v1/device/{deviceId}", deviceId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(deviceService).getDevice(deviceId);
        }

        @Test
        @WithUserDetails
        @DisplayName("Should return 404 Not Found when device does not exist")
        void getDevice_NotFound() throws Exception {
            when(deviceSecurity.hasPermission(deviceId, "READ")).thenThrow(new NotFoundException("Device not found."));

            mockMvc.perform(get("/api/v1/device/{deviceId}", deviceId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("Device not found.")));

            verify(deviceService, times(0)).getDevice(deviceId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/device/{deviceId}/attributes - getDeviceAttributes")
    class GetDeviceAttributesTests {
        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK with attribute list")
        void getDeviceAttributes_Success() throws Exception {
            List<AttributeTypeDto> attributes = List.of(new AttributeTypeDto());
            given(attributeService.getDeviceAttributes(deviceId)).willReturn(attributes);
            when(deviceSecurity.hasPermission(deviceId, "READ")).thenReturn(true);

            mockMvc.perform(get("/api/v1/device/{deviceId}/attributes", deviceId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(attributeService).getDeviceAttributes(deviceId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/device/{deviceId}/access - getDeviceAccess")
    class GetDeviceAccessTests {
        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK with access list when role filter is provided")
        void getDeviceAccess_WithRole() throws Exception {
            DeviceRole role = DeviceRole.values()[0];
            List<UserRoleDto> userRoles = List.of(new UserRoleDto());
            given(deviceService.getDeviceAccess(deviceId, role)).willReturn(userRoles);
            when(deviceSecurity.hasPermission(deviceId, "ADMIN")).thenReturn(true);

            mockMvc.perform(get("/api/v1/device/{deviceId}/access", deviceId)
                            .param("role", role.name())
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(deviceService).getDeviceAccess(deviceId, role);
        }

        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK when role parameter is omitted")
        void getDeviceAccess_WithoutRole() throws Exception {
            given(deviceService.getDeviceAccess(deviceId, null)).willReturn(Collections.emptyList());
            when(deviceSecurity.hasPermission(deviceId, "ADMIN")).thenReturn(true);

            mockMvc.perform(get("/api/v1/device/{deviceId}/access", deviceId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(deviceService).getDeviceAccess(deviceId, null);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/device/{deviceId}/access - removeDeviceAccess")
    class RemoveDeviceAccessTests {
        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK when access is successfully revoked")
        void removeDeviceAccess_Success() throws Exception {
            String email = "user@example.com";
            willDoNothing().given(deviceService).removeAccess(deviceId, email);
            when(deviceSecurity.hasPermission(deviceId, "ADMIN")).thenReturn(true);

            mockMvc.perform(delete("/api/v1/device/{deviceId}/access", deviceId)
                            .param("emailAddress", email)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader))
                    .andExpect(status().isOk());

            verify(deviceService).removeAccess(deviceId, email);
        }

        @Test
        @WithUserDetails
        @DisplayName("Should return 400 Bad Request when mandatory emailAddress parameter is missing")
        void removeDeviceAccess_MissingParam() throws Exception {
            when(deviceSecurity.hasPermission(deviceId, "ADMIN")).thenReturn(true);

            mockMvc.perform(delete("/api/v1/device/{deviceId}/access", deviceId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithUserDetails
        @DisplayName("Should return 404 Not Found when target user or device access record does not exist")
        void removeDeviceAccess_NotFound() throws Exception {
            String email = "unknown@example.com";
            when(deviceSecurity.hasPermission(deviceId, "ADMIN")).thenReturn(true);
            willThrow(new NotFoundException("User access record not found"))
                    .given(deviceService).removeAccess(deviceId, email);

            mockMvc.perform(delete("/api/v1/device/{deviceId}/access", deviceId)
                            .param("emailAddress", email)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("User access record not found")));

            verify(deviceService).removeAccess(deviceId, email);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/device/{deviceId}/data - getDeviceTelemetry")
    class GetDeviceTelemetryTests {
        @Test
        @WithUserDetails
        @DisplayName("Should parse query parameters, bind DTO and Pageable, and return 200 OK")
        void getDeviceTelemetry_Success() throws Exception {
            SensorDataDto mockSensorData = new SensorDataDto();
            given(sensorDataService.getDataByDevice(eq(deviceId), any(SensorDataFilterDto.class), any(Pageable.class)))
                    .willReturn(mockSensorData);
            when(deviceSecurity.hasPermission(deviceId, "READ")).thenReturn(true);

            mockMvc.perform(get("/api/v1/device/{deviceId}/data", deviceId)
                            .param("from", "2026-01-01T00:00:00Z")
                            .param("to", "2026-01-02T00:00:00Z")
                            .param("attributes", "temperature", "humidity")
                            .param("page", "0")
                            .param("size", "20")
                            .param("sort", "id.timestamp,desc")
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(sensorDataService).getDataByDevice(eq(deviceId), any(SensorDataFilterDto.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/device/{deviceId}/share - shareDevice")
    class ShareDeviceTests {
        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK when access is granted successfully")
        void shareDevice_Success() throws Exception {
            DeviceRole role = DeviceRole.values()[0];
            ShareRequestDto shareRequest = new ShareRequestDto();
            shareRequest.setTargetUserEmail("collaborator@example.com");
            shareRequest.setRole(role);

            when(deviceSecurity.hasPermission(deviceId, "ADMIN")).thenReturn(true);
            willDoNothing().given(deviceService).grantAccess(deviceId, "collaborator@example.com", role);

            mockMvc.perform(post("/api/v1/device/{deviceId}/share", deviceId)
                            .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(shareRequest)))
                    .andExpect(status().isOk());

            verify(deviceService).grantAccess(deviceId, "collaborator@example.com", role);
        }
    }
}