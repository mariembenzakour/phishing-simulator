package com.intellisec.phishsim.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SendEventRepository extends JpaRepository<SendEvent, UUID> {

    Optional<SendEvent> findByTrackingToken(String trackingToken);

    List<SendEvent> findByCampaignId(UUID campaignId);

    List<SendEvent> findByTargetId(UUID targetId);

    List<SendEvent> findByCampaignIdAndStatus(UUID campaignId, String status);

    // ✅ AJOUTÉ : Supprimer les SendEvent orphelins (sans TrackingEvent)
    @Modifying
    @Query("DELETE FROM SendEvent se " +
            "WHERE se.sentAt < :cutoff " +
            "AND NOT EXISTS (SELECT 1 FROM TrackingEvent te WHERE te.sendEventId = se.id)")
    long deleteOrphanSendEvents(@Param("cutoff") LocalDateTime cutoff);

    // ✅ AJOUTÉ : Compter les SendEvent par campagne avec statut
    @Query("SELECT COUNT(se) FROM SendEvent se WHERE se.campaignId = :campaignId AND se.status = :status")
    long countByCampaignIdAndStatus(@Param("campaignId") UUID campaignId, @Param("status") String status);
}