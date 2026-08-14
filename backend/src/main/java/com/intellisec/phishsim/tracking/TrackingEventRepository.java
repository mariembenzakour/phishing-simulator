package com.intellisec.phishsim.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {

    List<TrackingEvent> findBySendEventId(UUID sendEventId);

    List<TrackingEvent> findBySendEventIdAndEventType(UUID sendEventId, String eventType);

    List<TrackingEvent> findByEventType(String eventType);

    // ✅ AJOUTÉ : Supprimer les événements avant une date
    @Modifying
    @Query("DELETE FROM TrackingEvent te WHERE te.occurredAt < :cutoff")
    long deleteByOccurredAtBefore(@Param("cutoff") LocalDateTime cutoff);

    // ✅ AJOUTÉ : Compter les événements avant une date (pour monitoring)
    @Query("SELECT COUNT(te) FROM TrackingEvent te WHERE te.occurredAt < :cutoff")
    long countByOccurredAtBefore(@Param("cutoff") LocalDateTime cutoff);

    // ✅ Statistiques par utilisateur
    @Query("SELECT t.id, t.email, t.firstName, t.lastName, " +
            "COUNT(CASE WHEN te.eventType = 'OPEN' THEN 1 END), " +
            "COUNT(CASE WHEN te.eventType = 'CLICK' THEN 1 END), " +
            "COUNT(CASE WHEN te.eventType = 'SUBMIT' THEN 1 END) " +
            "FROM Target t " +
            "JOIN SendEvent se ON t.id = se.targetId " +
            "LEFT JOIN TrackingEvent te ON se.id = te.sendEventId " +
            "GROUP BY t.id, t.email, t.firstName, t.lastName")
    List<Object[]> getUserStats();

    // ✅ Historique d'un utilisateur par targetId
    @Query("SELECT te FROM TrackingEvent te " +
            "JOIN SendEvent se ON te.sendEventId = se.id " +
            "WHERE se.targetId = :targetId " +
            "ORDER BY te.occurredAt DESC")
    List<TrackingEvent> findUserHistory(@Param("targetId") UUID targetId);
}