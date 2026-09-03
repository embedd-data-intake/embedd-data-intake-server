package com.github.embedd_data_intake.server.model;

import com.github.embedd_data_intake.server.enums.AttributeType;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Table(name = "attributes", schema = "foreign_schema")
@Getter
@Immutable
public class Attribute {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String attributeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private AttributeType type;
}
