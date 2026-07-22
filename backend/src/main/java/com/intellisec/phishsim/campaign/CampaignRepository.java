package com.intellisec.phishsim.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findByCreatedBy(UUID createdBy);
    List<Campaign> findByStatusAndScheduledAtBefore(String status, LocalDateTime dateTime);
}