package com.github.embedd_data_intake.server.repository;

import com.github.embedd_data_intake.server.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailRepository extends JpaRepository<Email, UUID> {
    Optional<Email> findByEmailAddress(String emailAddress);

    boolean existsByEmailAddress(String emailAddress);
}
