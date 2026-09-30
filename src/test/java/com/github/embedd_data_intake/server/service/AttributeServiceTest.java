package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.AttributeTypeDto;
import com.github.embedd_data_intake.server.enums.AttributeType;
import com.github.embedd_data_intake.server.model.Attribute; // Assume standard entity model
import com.github.embedd_data_intake.server.repository.AttributeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttributeServiceTest {
    @Mock
    private AttributeRepository attributeRepository;

    @InjectMocks
    private AttributeService attributeService;

    @Nested
    @DisplayName("getDeviceAttributes()")
    class GetDeviceAttributesTests {
        @Test
        void getDeviceAttributes_WhenAttributesExist_ReturnsAttributeTypeDtoList() {
            UUID deviceId = UUID.randomUUID();

            Attribute attr1 = mock(Attribute.class);
            given(attr1.getAttributeName()).willReturn("temperature");
            given(attr1.getType()).willReturn(AttributeType.NUMBER);

            Attribute attr2 = mock(Attribute.class);
            given(attr2.getAttributeName()).willReturn("firmware_version");
            given(attr2.getType()).willReturn(AttributeType.STRING);

            given(attributeRepository.findAttributesByDeviceId(deviceId))
                    .willReturn(List.of(attr1, attr2));

            List<AttributeTypeDto> result = attributeService.getDeviceAttributes(deviceId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAttributeName()).isEqualTo("temperature");
            assertThat(result.get(0).getType()).isEqualTo(AttributeType.NUMBER);
            assertThat(result.get(1).getAttributeName()).isEqualTo("firmware_version");
            assertThat(result.get(1).getType()).isEqualTo(AttributeType.STRING);

            verify(attributeRepository).findAttributesByDeviceId(deviceId);
        }

        @Test
        void getDeviceAttributes_WhenNoAttributesFound_ReturnsEmptyList() {
            UUID deviceId = UUID.randomUUID();
            given(attributeRepository.findAttributesByDeviceId(deviceId))
                    .willReturn(Collections.emptyList());

            List<AttributeTypeDto> result = attributeService.getDeviceAttributes(deviceId);

            assertThat(result).isEmpty();
            verify(attributeRepository).findAttributesByDeviceId(deviceId);
        }
    }

    @Nested
    @DisplayName("getUserAttributes()")
    class GetUserAttributesTests {
        @Test
        void getUserAttributes_WhenAttributesExist_ReturnsMappedDtoList() {
            UUID userId = UUID.randomUUID();

            Attribute attr = mock(Attribute.class);
            given(attr.getAttributeName()).willReturn("humidity");
            given(attr.getType()).willReturn(AttributeType.NUMBER);

            given(attributeRepository.findDistinctAttributesByUserId(userId))
                    .willReturn(List.of(attr));

            List<AttributeTypeDto> result = attributeService.getUserAttributes(userId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getAttributeName()).isEqualTo("humidity");
            assertThat(result.getFirst().getType()).isEqualTo(AttributeType.NUMBER);

            verify(attributeRepository).findDistinctAttributesByUserId(userId);
        }

        @Test
        void getUserAttributes_WhenNoAttributesFound_ReturnsEmptyList() {
            UUID userId = UUID.randomUUID();
            given(attributeRepository.findDistinctAttributesByUserId(userId))
                    .willReturn(Collections.emptyList());

            List<AttributeTypeDto> result = attributeService.getUserAttributes(userId);

            assertThat(result).isEmpty();
            verify(attributeRepository).findDistinctAttributesByUserId(userId);
        }
    }
}
