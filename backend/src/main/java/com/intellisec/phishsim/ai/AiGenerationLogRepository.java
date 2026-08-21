package com.intellisec.phishsim.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    List<AiGenerationLog> findByGeneratedBy(String generatedBy);

    List<AiGenerationLog> findByApproved(Boolean approved);

    List<AiGenerationLog> findByScenario(String scenario);
}