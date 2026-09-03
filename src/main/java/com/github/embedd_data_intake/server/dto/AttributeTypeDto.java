package com.github.embedd_data_intake.server.dto;

import com.github.embedd_data_intake.server.enums.AttributeType;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AttributeTypeDto {
    private String attributeName;
    private AttributeType type;
}
