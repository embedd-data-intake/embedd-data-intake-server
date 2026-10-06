package com.github.embedd_data_intake.server.controller;

import com.github.embedd_data_intake.server.dto.*;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.impl.NotFoundException;
import com.github.embedd_data_intake.server.handler.RestExceptionHandler;
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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(RestExceptionHandler.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AttributeService attributeService;

    @MockitoBean
    private SensorDataService sensorDataService;

    private UUID userId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    void authenticatedSetup() {
        when(jwtService.extractSessionId(anyString())).thenReturn(sessionId);
        when(refreshTokenService.existsById(sessionId)).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(userId);
    }

    @Nested
    @DisplayName("GET /api/v1/user - getUser")
    class GetUserTests {
        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK with UserDto when user is found")
        void getUser_Success() throws Exception {
            authenticatedSetup();

            mockMvc.perform(get("/api/v1/user")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                            .principal(() -> userId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(userService).getUser(userId);
        }

        @Test
        @WithUserDetails
        @DisplayName("Should return status from RestException and ExceptionDetailsDto payload")
        void getUser_RestException() throws Exception {
            authenticatedSetup();

            given(userService.getUser(userId))
                    .willThrow(new NotFoundException("User not found"));


            mockMvc.perform(get("/api/v1/user")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                            .principal(() -> userId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("User not found")))
                    .andExpect(jsonPath("$.details", containsString("uri=/api/v1/user")));

            verify(userService).getUser(userId);
        }

        @Test
        @WithUserDetails
        @DisplayName("Should return 500 Internal Server Error for unhandled exceptions")
        void getUser_UnhandledException() throws Exception {
            authenticatedSetup();

            given(userService.getUser(userId))
                    .willThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/v1/user")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                            .principal(() -> userId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message", is("Internal server error")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/user/attributes - getUserAttributes")
    class GetUserAttributesTests {

        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK with attribute list")
        void getUserAttributes_Success() throws Exception {
            authenticatedSetup();

            List<AttributeTypeDto> attributes = List.of(new AttributeTypeDto());
            given(attributeService.getUserAttributes(userId)).willReturn(attributes);

            mockMvc.perform(get("/api/v1/user/attributes")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                            .principal(() -> userId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(attributeService).getUserAttributes(userId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/user/device - getDevices")
    class GetDevicesTests {
        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK when role parameter is provided")
        void getDevices_WithRoleParam() throws Exception {
            authenticatedSetup();

            DeviceRole role = DeviceRole.values()[0]; // Resolves dynamically from your enum
            List<DeviceRoleDto> devices = List.of(new DeviceRoleDto());
            given(userService.getUserDevices(userId, role)).willReturn(devices);

            mockMvc.perform(get("/api/v1/user/device")
                            .param("role", role.name())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                            .principal(() -> userId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(userService).getUserDevices(userId, role);
        }

        @Test
        @WithUserDetails
        @DisplayName("Should return 200 OK when role parameter is omitted")
        void getDevices_WithoutRoleParam() throws Exception {
            authenticatedSetup();
            given(userService.getUserDevices(userId, null)).willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/user/device")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                            .principal(() -> userId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(userService).getUserDevices(userId, null);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/user/data - getUserTelemetry")
    class GetUserTelemetryTests {
        @Test
        @WithUserDetails
        @DisplayName("Should parse query parameters and return Map<UUID, List<SensorDataCollectionDto>>")
        void getUserTelemetry_Success() throws Exception {
            authenticatedSetup();

            UUID deviceId = UUID.randomUUID();
            Map<UUID, List<SensorDataCollectionDto>> responseMap = Map.of(
                    deviceId, Collections.emptyList()
            );

            given(sensorDataService.getDataByUser(eq(userId), any(SensorDataFilterDto.class), any(Pageable.class)))
                    .willReturn(responseMap);

            mockMvc.perform(get("/api/v1/user/data")
                            .param("from", "2026-01-01T00:00:00Z")
                            .param("to", "2026-01-02T00:00:00Z")
                            .param("attributes", "temp", "humidity")
                            .param("page", "0")
                            .param("size", "20")
                            .param("sort", "id.timestamp,desc")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                            .principal(() -> userId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.['" + deviceId + "']").exists());

            verify(sensorDataService).getDataByUser(eq(userId), any(SensorDataFilterDto.class), any(Pageable.class));
        }
    }
}
