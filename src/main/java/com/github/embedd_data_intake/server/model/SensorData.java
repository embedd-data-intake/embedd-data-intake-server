package com.github.embedd_data_intake.server.model;

import com.github.embedd_data_intake.server.enums.AttributeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "sensor_data", schema = "foreign_schema")
@Getter
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SensorData {
    @EmbeddedId
    private SensorDataId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id")
    private Attribute attribute;

    @Column(name = "val_num")
    private Double valNum;

    @Column(name = "val_string", length = 64)
    private String valString;

    @Column(name = "val_boolean")
    private Boolean valBoolean;

    @SuppressWarnings("unchecked")
    public <T> T getVal(@NotNull AttributeType attributeType) {
        Object value = switch (attributeType) {
            case NUMBER -> valNum;
            case STRING -> valString;
            case BOOLEAN -> valBoolean;
        };

        return (T) value;
    }
}
