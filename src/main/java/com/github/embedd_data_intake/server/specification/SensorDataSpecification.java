package com.github.embedd_data_intake.server.specification;

import com.github.embedd_data_intake.server.dto.SensorDataFilterDto;
import com.github.embedd_data_intake.server.model.SensorData;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SensorDataSpecification {
    public static Specification<SensorData> withFilter(UUID deviceId, SensorDataFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("id").get("deviceId"), deviceId));

            if (!Objects.isNull(filter.getFrom())) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("id").get("timestamp"), filter.getFrom()));
            }

            if (!Objects.isNull(filter.getTo())) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("id").get("timestamp"), filter.getTo()));
            }

            if (filter.getAttributes() != null && !filter.getAttributes().isEmpty()) {
                predicates.add(root.join("attribute").get("attributeName").in(filter.getAttributes()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
