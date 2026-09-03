package com.github.embedd_data_intake.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AttributeType {
    NUMBER(Double.class),
    BOOLEAN(Boolean.class),
    STRING(String.class);

    private final Class<?> type;
}
