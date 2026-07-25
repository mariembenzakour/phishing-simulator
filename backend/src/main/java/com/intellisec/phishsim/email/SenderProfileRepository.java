package com.intellisec.phishsim.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SenderProfileRepository extends JpaRepository<SenderProfile, UUID> {
}