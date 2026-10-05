package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.DeviceDto;
import com.github.embedd_data_intake.server.dto.UserDto;
import com.github.embedd_data_intake.server.dto.UserRoleDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.impl.BadRequestException;
import com.github.embedd_data_intake.server.exceptions.impl.NotFoundException;
import com.github.embedd_data_intake.server.model.Device;
import com.github.embedd_data_intake.server.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {
    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserDeviceService userDeviceService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DeviceService deviceService;

    // Helper method to build a real Device entity without depending on Lombok/setters for ID
    private Device createDevice(UUID id, String deviceName) {
        Device device = new Device();
        ReflectionTestUtils.setField(device, "id", id);
        device.setDeviceName(deviceName);
        return device;
    }

    @Nested
    @DisplayName("grantAccess()")
    class GrantAccessTests {
        @Test
        void grantAccess_WhenUserAndDeviceExist_GrantsAccess() throws NotFoundException {
            UUID deviceId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String targetEmail = "user@example.com";
            DeviceRole role = DeviceRole.ADMIN;

            UserDto mockUser = new UserDto(userId, targetEmail);
            Device mockDevice = createDevice(deviceId, "Smart Thermostat");

            given(userService.getUser(targetEmail)).willReturn(mockUser);
            given(deviceRepository.findById(deviceId)).willReturn(Optional.of(mockDevice));

            deviceService.grantAccess(deviceId, targetEmail, role);

            verify(userService).getUser(targetEmail);
            verify(deviceRepository).findById(deviceId);
            verify(userDeviceService).grantAccess(userId, deviceId, role);
        }

        @Test
        void grantAccess_WhenUserNotFound_ThrowsNotFoundException() {
            UUID deviceId = UUID.randomUUID();
            String targetEmail = "missing@example.com";

            given(userService.getUser(targetEmail)).willThrow(new NotFoundException("User not found."));

            assertThatThrownBy(() -> deviceService.grantAccess(deviceId, targetEmail, DeviceRole.ADMIN))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found.");

            verify(deviceRepository, never()).findById(any());
            verify(userDeviceService, never()).grantAccess(any(), any(), any());
        }

        @Test
        void grantAccess_WhenDeviceNotFound_ThrowsNotFoundException() {
            UUID deviceId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String targetEmail = "user@example.com";

            UserDto mockUser = new UserDto(userId, targetEmail);
            given(userService.getUser(targetEmail)).willReturn(mockUser);
            given(deviceRepository.findById(deviceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.grantAccess(deviceId, targetEmail, DeviceRole.READ))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Device not found");

            verify(userDeviceService, never()).grantAccess(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("removeAccess()")
    class RemoveAccessTests {

        @Test
        void removeAccess_WhenUserAndDeviceExist_RemovesAccess() throws NotFoundException, BadRequestException {
            UUID deviceId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String targetEmail = "user@example.com";

            UserDto mockUser = new UserDto(userId, targetEmail);
            Device mockDevice = createDevice(deviceId, "Security Camera");

            given(userService.getUser(targetEmail)).willReturn(mockUser);
            given(deviceRepository.findById(deviceId)).willReturn(Optional.of(mockDevice));

            deviceService.removeAccess(deviceId, targetEmail);

            verify(userService).getUser(targetEmail);
            verify(deviceRepository).findById(deviceId);
            verify(userDeviceService).removeAccess(userId, deviceId);
        }

        @Test
        void removeAccess_WhenUserDeviceServiceThrowsBadRequest_PropagatesException() throws Exception {
            UUID deviceId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String targetEmail = "owner@example.com";

            UserDto mockUser = new UserDto(userId, targetEmail);
            Device mockDevice = createDevice(deviceId, "Gateway");

            given(userService.getUser(targetEmail)).willReturn(mockUser);
            given(deviceRepository.findById(deviceId)).willReturn(Optional.of(mockDevice));
            doThrow(new BadRequestException("Cannot remove access for OWNER"))
                    .when(userDeviceService).removeAccess(userId, deviceId);

            assertThatThrownBy(() -> deviceService.removeAccess(deviceId, targetEmail))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cannot remove access for OWNER");
        }

        @Test
        void removeAccess_WhenDeviceNotFound_ThrowsNotFoundException() {
            UUID deviceId = UUID.randomUUID();
            String targetEmail = "user@example.com";

            given(userService.getUser(targetEmail)).willReturn(new UserDto(UUID.randomUUID(), targetEmail));
            given(deviceRepository.findById(deviceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.removeAccess(deviceId, targetEmail))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Device not found");

            verify(userDeviceService, never()).removeAccess(any(), any());
        }
    }

    @Nested
    @DisplayName("addDevice()")
    class AddDeviceTests {

        @Test
        void addDevice_WhenUserExists_SavesDeviceAndAssignsOwnerRole() throws NotFoundException {
            UUID ownerId = UUID.randomUUID();
            String deviceName = "Weather Sensor";
            UserDto mockUser = new UserDto(ownerId, "owner@example.com");

            given(userService.getUser(ownerId)).willReturn(mockUser);

            doAnswer(invocation -> {
                Device savedDevice = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedDevice, "id", UUID.randomUUID());
                return savedDevice;
            }).when(deviceRepository).save(any(Device.class));

            UUID createdDeviceId = deviceService.addDevice(ownerId, deviceName);

            assertThat(createdDeviceId).isNotNull();
            verify(userService).getUser(ownerId);
            verify(deviceRepository).save(argThat(device -> deviceName.equals(device.getDeviceName())));
            verify(userDeviceService).addDeviceToUser(eq(ownerId), eq(createdDeviceId), eq(DeviceRole.OWNER));
        }

        @Test
        void addDevice_WhenUserNotFound_ThrowsNotFoundException() {
            UUID ownerId = UUID.randomUUID();
            given(userService.getUser(ownerId)).willThrow(new NotFoundException("User not found."));

            assertThatThrownBy(() -> deviceService.addDevice(ownerId, "New Sensor"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found.");

            verify(deviceRepository, never()).save(any());
            verify(userDeviceService, never()).addDeviceToUser(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getDevice()")
    class GetDeviceTests {

        @Test
        void getDevice_WhenDeviceExists_ReturnsDeviceDto() throws NotFoundException {
            UUID deviceId = UUID.randomUUID();
            Device mockDevice = createDevice(deviceId, "Smart Hub");

            given(deviceRepository.findById(deviceId)).willReturn(Optional.of(mockDevice));

            DeviceDto result = deviceService.getDevice(deviceId);

            assertThat(result).isNotNull();
            verify(deviceRepository).findById(deviceId);
        }

        @Test
        void getDevice_WhenDeviceNotFound_ThrowsNotFoundException() {
            UUID deviceId = UUID.randomUUID();
            given(deviceRepository.findById(deviceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.getDevice(deviceId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Device not found.");
        }
    }

    @Nested
    @DisplayName("getDeviceAccess()")
    class GetDeviceAccessTests {

        @Test
        void getDeviceAccess_WhenRoleIsNull_CallsGetAccessForDeviceWithoutRole() {
            UUID deviceId = UUID.randomUUID();
            UserRoleDto mockUserRole = mock(UserRoleDto.class);

            given(emailService.getAccessForDevice(deviceId)).willReturn(List.of(mockUserRole));

            List<UserRoleDto> result = deviceService.getDeviceAccess(deviceId, null);

            assertThat(result).hasSize(1);
            verify(emailService).getAccessForDevice(deviceId);
            verify(emailService, never()).getAccessForDevice(any(), any());
        }

        @Test
        void getDeviceAccess_WhenRoleProvided_CallsGetAccessForDeviceWithRole() {
            UUID deviceId = UUID.randomUUID();
            DeviceRole role = DeviceRole.OWNER;
            UserRoleDto mockUserRole = mock(UserRoleDto.class);

            given(emailService.getAccessForDevice(deviceId, role)).willReturn(List.of(mockUserRole));

            List<UserRoleDto> result = deviceService.getDeviceAccess(deviceId, role);

            assertThat(result).hasSize(1);
            verify(emailService).getAccessForDevice(deviceId, role);
            verify(emailService, never()).getAccessForDevice(deviceId);
        }
    }
}