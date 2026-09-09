package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Traverses User -> userEmails -> email -> emailAddress
    @EntityGraph(attributePaths = {"userEmails", "userEmails.email"})
    Optional<User> findByUserEmails_Email_EmailAddress(String emailAddress);

    boolean existsByUserEmails_Email_EmailAddress(String emailAddress);

    // Eagerly fetches active devices to prevent N+1 queries
    @EntityGraph(attributePaths = {"userDevices", "userDevices.device"})
    Optional<User> findWithDevicesById(UUID deviceId);

    // Eagerly fetches active emails to prevent N+1 queries
    @EntityGraph(attributePaths = {"userEmails", "userEmails.email"})
    Optional<User> findWithEmailsById(UUID emailId);

    @EntityGraph(attributePaths = {"refreshTokens", "refreshTokens.token"})
    Optional<User> findByRefreshTokens_Token(UUID token);
}
