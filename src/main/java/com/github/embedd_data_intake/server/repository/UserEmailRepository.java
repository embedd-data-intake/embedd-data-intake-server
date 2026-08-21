package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.model.UserEmail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserEmailRepository extends JpaRepository<UserEmail, UUID> {

    @EntityGraph(attributePaths = {"email"})
    List<UserEmail> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"user", "email"})
    Optional<UserEmail> findByEmail_EmailAddress(String emailAddress);

    Optional<UserEmail> findByUserIdAndEmail_Id(UUID userId, UUID emailId);

    // Calling delete triggers the @SQLDelete soft-delete annotation
    void deleteByUserIdAndEmail_Id(UUID userId, UUID emailId);

    // Checks if an active user_email record exists for this email address
    boolean existsByEmail_EmailAddress(String emailAddress);
}
