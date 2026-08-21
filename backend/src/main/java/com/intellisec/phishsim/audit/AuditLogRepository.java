package com.intellisec.phishsim.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByActor(String actor);

    List<AuditLog> findByTarget(String target);

    List<AuditLog> findByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = 'CAMPAIGN_SEND'")
    long countTotalSends();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = 'CAMPAIGN_SEND' AND a.createdAt > :since")
    long countSendsSince(@Param("since") LocalDateTime since);
}