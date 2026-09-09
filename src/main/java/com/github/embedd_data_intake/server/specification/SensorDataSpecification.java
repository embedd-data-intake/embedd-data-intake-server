package com.github.embedd_data_intake.server.specification;

import com.github.embedd_data_intake.server.dto.SensorDataFilterDto;
import com.github.embedd_data_intake.server.model.SensorData;
import com.github.embedd_data_intake.server.model.UserDevice;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SensorDataSpecification {
    private static List<Predicate> getBasePredicates(
            SensorDataFilterDto filter,
            Root<SensorData> root,
            CriteriaBuilder criteriaBuilder
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (!Objects.isNull(filter.getFrom())) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("id").get("timestamp"), filter.getFrom()));
        }

        if (!Objects.isNull(filter.getTo())) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("id").get("timestamp"), filter.getTo()));
        }

        if (filter.getAttributes() != null && !filter.getAttributes().isEmpty()) {
            predicates.add(root.join("attribute").get("attributeName").in(filter.getAttributes()));
        }

        return predicates;
    }

    public static Specification<SensorData> withDeviceFilter(UUID deviceId, SensorDataFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = getBasePredicates(filter, root, criteriaBuilder);

            predicates.add(criteriaBuilder.equal(root.get("id").get("deviceId"), deviceId));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<SensorData> withUserFilter(UUID userId, SensorDataFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = getBasePredicates(filter, root, criteriaBuilder);

            // 1. Explicit Join on unrelated entities matching SensorDataId.deviceId == UserDevice.deviceId
            Join<SensorData, UserDevice> deviceJoin = root.join(UserDevice.class, JoinType.INNER);
            deviceJoin.on(criteriaBuilder.equal(root.get("id").get("deviceId"), deviceJoin.get("device").get("id")));

            // 2. Filter by userId and role on UserDevice
            predicates.add(criteriaBuilder.equal(deviceJoin.get("user").get("id"), userId));
//            predicates.add(criteriaBuilder.equal(deviceJoin.get("role"), role)); // Adjust 'role' field name if needed

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
