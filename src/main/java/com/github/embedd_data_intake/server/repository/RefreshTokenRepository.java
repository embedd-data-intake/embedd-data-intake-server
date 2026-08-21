package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.model.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Optional<RefreshToken> findByToken(UUID token);

    List<RefreshToken> findByUserId(UUID userId);

    void deleteByToken(String token);

    void deleteByUserId(UUID userId);

    // Cleans up expired tokens in bulk
    void deleteByExpiresAtBefore(OffsetDateTime now);
}
