package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.UserDeviceDto;
import com.github.embedd_data_intake.server.enums.DeviceRole;
import com.github.embedd_data_intake.server.exceptions.BadRequestException;
import com.github.embedd_data_intake.server.exceptions.NotFoundException;
import com.github.embedd_data_intake.server.model.UserDevice;
import com.github.embedd_data_intake.server.repository.UserDeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private UserDeviceService userDeviceService;

    // Helper method to create a real UserDevice entity
    private UserDevice createUserDevice(UUID id, UUID userId, UUID deviceId, DeviceRole role, OffsetDateTime deletedAt) {
        UserDevice userDevice = new UserDevice();
        ReflectionTestUtils.setField(userDevice, "id", id);
        userDevice.setUserId(userId);
        userDevice.setDeviceId(deviceId);
        userDevice.setRole(role);
        userDevice.setDeletedAt(deletedAt);
        return userDevice;
    }

    @Nested
    @DisplayName("addDeviceToUser()")
    class AddDeviceToUserTests {
        @Test
        void addDeviceToUser_SavesUserDeviceWithGivenRole() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            DeviceRole role = DeviceRole.OWNER;

            userDeviceService.addDeviceToUser(userId, deviceId, role);

            ArgumentCaptor<UserDevice> captor = ArgumentCaptor.forClass(UserDevice.class);
            verify(userDeviceRepository).save(captor.capture());

            UserDevice saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getDeviceId()).isEqualTo(deviceId);
            assertThat(saved.getRole()).isEqualTo(role);
        }
    }

    @Nested
    @DisplayName("grantAccess()")
    class GrantAccessTests {

        @Test
        void grantAccess_WhenAssociationExists_UpdatesRoleAndClearsDeletedAt() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            UUID userDeviceId = UUID.randomUUID();
            DeviceRole newRole = DeviceRole.ADMIN;
            OffsetDateTime previousDeletedAt = OffsetDateTime.now().minusDays(2);

            UserDevice existingUserDevice = createUserDevice(userDeviceId, userId, deviceId, DeviceRole.READ, previousDeletedAt);

            given(userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                    .willReturn(Optional.of(existingUserDevice));

            userDeviceService.grantAccess(userId, deviceId, newRole);

            ArgumentCaptor<UserDevice> captor = ArgumentCaptor.forClass(UserDevice.class);
            verify(userDeviceRepository).save(captor.capture());

            UserDevice saved = captor.getValue();
            assertThat(saved.getId()).isEqualTo(userDeviceId);
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getDeviceId()).isEqualTo(deviceId);
            assertThat(saved.getRole()).isEqualTo(newRole);
            assertThat(saved.getDeletedAt()).isNull();
        }

        @Test
        void grantAccess_WhenAssociationDoesNotExist_CreatesNewAndSaves() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            DeviceRole role = DeviceRole.READ;

            given(userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                    .willReturn(Optional.empty());

            userDeviceService.grantAccess(userId, deviceId, role);

            ArgumentCaptor<UserDevice> captor = ArgumentCaptor.forClass(UserDevice.class);
            verify(userDeviceRepository).save(captor.capture());

            UserDevice saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getDeviceId()).isEqualTo(deviceId);
            assertThat(saved.getRole()).isEqualTo(role);
            assertThat(saved.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("removeAccess()")
    class RemoveAccessTests {
        @Test
        void removeAccess_WhenUserIsNotOwner_SoftDeletesAssociation() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            UserDevice existing = createUserDevice(UUID.randomUUID(), userId, deviceId, DeviceRole.READ, null);

            given(userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                    .willReturn(Optional.of(existing));

            userDeviceService.removeAccess(userId, deviceId);

            ArgumentCaptor<UserDevice> captor = ArgumentCaptor.forClass(UserDevice.class);
            verify(userDeviceRepository).save(captor.capture());

            UserDevice saved = captor.getValue();
            assertThat(saved.getDeletedAt()).isNotNull();
            assertThat(saved.getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
        }

        @Test
        void removeAccess_WhenUserIsOwner_ThrowsBadRequestException() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            UserDevice ownerAssociation = createUserDevice(UUID.randomUUID(), userId, deviceId, DeviceRole.OWNER, null);

            given(userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                    .willReturn(Optional.of(ownerAssociation));

            assertThatThrownBy(() -> userDeviceService.removeAccess(userId, deviceId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cannot remove the owner of the device.");

            verify(userDeviceRepository, never()).save(any());
        }

        @Test
        void removeAccess_WhenDeviceNotFound_ThrowsNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();

            given(userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userDeviceService.removeAccess(userId, deviceId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Device not found.");

            verify(userDeviceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findByUserId() & findByUserIdAndRole()")
    class FindTests {
        @Test
        void findByUserId_ReturnsMappedDtos() {
            UUID userId = UUID.randomUUID();
            UserDevice ud1 = createUserDevice(UUID.randomUUID(), userId, UUID.randomUUID(), DeviceRole.OWNER, null);
            UserDevice ud2 = createUserDevice(UUID.randomUUID(), userId, UUID.randomUUID(), DeviceRole.READ, null);

            given(userDeviceRepository.findByUserId(userId)).willReturn(List.of(ud1, ud2));

            List<UserDeviceDto> result = userDeviceService.findByUserId(userId);

            assertThat(result).hasSize(2);
            verify(userDeviceRepository).findByUserId(userId);
        }

        @Test
        void findByUserIdAndRole_ReturnsMappedDtos() {
            UUID userId = UUID.randomUUID();
            DeviceRole role = DeviceRole.ADMIN;
            UserDevice ud = createUserDevice(UUID.randomUUID(), userId, UUID.randomUUID(), role, null);

            given(userDeviceRepository.findByUserIdAndRole(userId, role)).willReturn(List.of(ud));

            List<UserDeviceDto> result = userDeviceService.findByUserIdAndRole(userId, role);

            assertThat(result).hasSize(1);
            verify(userDeviceRepository).findByUserIdAndRole(userId, role);
        }
    }

    @Nested
    @DisplayName("userHasPermission()")
    class UserHasPermissionTests {

        @Test
        void userHasPermission_WhenUserHasSufficientRole_ReturnsTrue() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            DeviceRole mockRole = mock(DeviceRole.class);
            UserDevice userDevice = createUserDevice(UUID.randomUUID(), userId, deviceId, mockRole, null);

            given(userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                    .willReturn(Optional.of(userDevice));
            given(mockRole.hasPermission(DeviceRole.READ)).willReturn(true);

            boolean hasPerm = userDeviceService.userHasPermission(userId, deviceId, DeviceRole.READ);

            assertThat(hasPerm).isTrue();
            verify(mockRole).hasPermission(DeviceRole.READ);
        }

        @Test
        void userHasPermission_WhenUserLacksSufficientRole_ReturnsFalse() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();
            DeviceRole mockRole = mock(DeviceRole.class);
            UserDevice userDevice = createUserDevice(UUID.randomUUID(), userId, deviceId, mockRole, null);

            given(userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                    .willReturn(Optional.of(userDevice));
            given(mockRole.hasPermission(DeviceRole.ADMIN)).willReturn(false);

            boolean hasPerm = userDeviceService.userHasPermission(userId, deviceId, DeviceRole.ADMIN);

            assertThat(hasPerm).isFalse();
            verify(mockRole).hasPermission(DeviceRole.ADMIN);
        }

        @Test
        void userHasPermission_WhenDeviceNotFound_ThrowsNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID deviceId = UUID.randomUUID();

            given(userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userDeviceService.userHasPermission(userId, deviceId, DeviceRole.READ))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Device not found.");
        }
    }
}
