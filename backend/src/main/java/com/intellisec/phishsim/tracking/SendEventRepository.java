package com.intellisec.phishsim.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SendEventRepository extends JpaRepository<SendEvent, UUID> {
    Optional<SendEvent> findByTrackingToken(String trackingToken);
    List<SendEvent> findByCampaignId(UUID campaignId);
    List<SendEvent> findByTargetId(UUID targetId);
}