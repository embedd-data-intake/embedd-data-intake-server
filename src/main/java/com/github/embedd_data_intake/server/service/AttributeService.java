package com.github.embedd_data_intake.server.service;

import com.github.embedd_data_intake.server.dto.AttributeTypeDto;
import com.github.embedd_data_intake.server.repository.AttributeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AttributeService {
    private final AttributeRepository attributeRepository;

    public AttributeService(AttributeRepository attributeRepository) {
        this.attributeRepository = attributeRepository;
    }

    public List<AttributeTypeDto> getDeviceAttributes(UUID deviceId) {
        return attributeRepository.findAttributesByDeviceId(deviceId).stream()
                .map(attribute -> new AttributeTypeDto(attribute.getAttributeName(), attribute.getType()))
                .toList();
    }

    public List<AttributeTypeDto> getUserAttributes(UUID userId) {
        return attributeRepository.findDistinctAttributesByUserId(userId).stream()
                .map(attribute -> new AttributeTypeDto(attribute.getAttributeName(), attribute.getType()))
                .toList();
    }
}
