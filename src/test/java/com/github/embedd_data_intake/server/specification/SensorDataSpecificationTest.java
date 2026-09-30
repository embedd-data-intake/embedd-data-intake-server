package com.github.embedd_data_intake.server.specification;

import com.github.embedd_data_intake.server.dto.SensorDataFilterDto;
import com.github.embedd_data_intake.server.model.SensorData;
import com.github.embedd_data_intake.server.model.UserDevice;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class SensorDataSpecificationTest {
    @Mock
    private Root<SensorData> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Path idPath;

    @Mock
    private Path<OffsetDateTime> timestampPath;

    @Mock
    private Path<UUID> deviceIdPath;

    @Mock
    private Path attributeNamePath;

    @Mock
    private CriteriaBuilder.In inClause;

    @Mock
    private Predicate combinedPredicate;

    @Nested
    @DisplayName("withDeviceFilter()")
    class WithDeviceFilterTests {
        @Test
        void withDeviceFilter_AppliesDeviceIdPredicate() {
            UUID deviceId = UUID.randomUUID();
            SensorDataFilterDto filter = new SensorDataFilterDto();
            Predicate devicePredicate = mock(Predicate.class);

            given(root.get("id")).willReturn(idPath);
            given(idPath.get("deviceId")).willReturn(deviceIdPath);
            given(criteriaBuilder.equal(deviceIdPath, deviceId)).willReturn(devicePredicate);
            given(criteriaBuilder.and(any(Predicate[].class))).willReturn(combinedPredicate);

            Specification<SensorData> spec = SensorDataSpecification.withDeviceFilter(deviceId, filter);

            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            assertThat(result).isEqualTo(combinedPredicate);
            verify(criteriaBuilder).equal(deviceIdPath, deviceId);
        }

        @Test
        void withDeviceFilter_AppliesBasePredicatesWhenFilterProvided() {
            UUID deviceId = UUID.randomUUID();
            OffsetDateTime from = OffsetDateTime.now().minusDays(1);
            OffsetDateTime to = OffsetDateTime.now();
            List<String> attributes = List.of("temperature", "humidity");

            SensorDataFilterDto filter = new SensorDataFilterDto();
            filter.setFrom(from);
            filter.setTo(to);
            filter.setAttributes(attributes);

            Join attributeJoin = mock(Join.class);
            Predicate greaterThanOrEqualPredicate = mock(Predicate.class);
            Predicate lessThanOrEqualPredicate = mock(Predicate.class);
            Predicate devicePredicate = mock(Predicate.class);

            given(root.get("id")).willReturn(idPath);
            given(idPath.get("timestamp")).willReturn(timestampPath);
            given(idPath.get("deviceId")).willReturn(deviceIdPath);

            given(criteriaBuilder.greaterThanOrEqualTo(timestampPath, from)).willReturn(greaterThanOrEqualPredicate);
            given(criteriaBuilder.lessThanOrEqualTo(timestampPath, to)).willReturn(lessThanOrEqualPredicate);
            given(criteriaBuilder.equal(deviceIdPath, deviceId)).willReturn(devicePredicate);

            given(root.join("attribute")).willReturn(attributeJoin);
            given(attributeJoin.get("attributeName")).willReturn(attributeNamePath);
            given(attributeNamePath.in(attributes)).willReturn(inClause);

            given(criteriaBuilder.and(any(Predicate[].class))).willReturn(combinedPredicate);

            Specification<SensorData> spec = SensorDataSpecification.withDeviceFilter(deviceId, filter);

            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            assertThat(result).isEqualTo(combinedPredicate);
            verify(criteriaBuilder).greaterThanOrEqualTo(timestampPath, from);
            verify(criteriaBuilder).lessThanOrEqualTo(timestampPath, to);
            verify(attributeNamePath).in(attributes);
            verify(criteriaBuilder).equal(deviceIdPath, deviceId);
        }
    }

    @Nested
    @DisplayName("withUserFilter()")
    class WithUserFilterTests {

        @Test
        void withUserFilter_JoinsUserDeviceAndAppliesUserFilter() {
            UUID userId = UUID.randomUUID();
            SensorDataFilterDto filter = new SensorDataFilterDto();

            Join deviceJoin = mock(Join.class);
            Path devicePath = mock(Path.class);
            Path deviceIdFromJoin = mock(Path.class);
            Path userPath = mock(Path.class);
            Path userIdPath = mock(Path.class);

            Predicate onPredicate = mock(Predicate.class);
            Predicate userPredicate = mock(Predicate.class);

            given(root.get("id")).willReturn(idPath);
            given(idPath.get("deviceId")).willReturn(deviceIdPath);

            given(root.join(UserDevice.class, JoinType.INNER)).willReturn(deviceJoin);
            given(deviceJoin.get("device")).willReturn(devicePath);
            given(devicePath.get("id")).willReturn(deviceIdFromJoin);
            given(criteriaBuilder.equal(deviceIdPath, deviceIdFromJoin)).willReturn(onPredicate);

            given(deviceJoin.get("user")).willReturn(userPath);
            given(userPath.get("id")).willReturn(userIdPath);
            given(criteriaBuilder.equal(userIdPath, userId)).willReturn(userPredicate);

            given(criteriaBuilder.and(any(Predicate[].class))).willReturn(combinedPredicate);

            Specification<SensorData> spec = SensorDataSpecification.withUserFilter(userId, filter);

            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            assertThat(result).isEqualTo(combinedPredicate);
            verify(deviceJoin).on(onPredicate);
            verify(criteriaBuilder).equal(userIdPath, userId);
        }
    }
}
