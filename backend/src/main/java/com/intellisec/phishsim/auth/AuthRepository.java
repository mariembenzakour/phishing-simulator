package com.intellisec.phishsim.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AuthRepository extends JpaRepository<Operator, UUID> {
    Optional<Operator> findByEmail(String email);
}
