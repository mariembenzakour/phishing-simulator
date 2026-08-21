package com.intellisec.phishsim.audit;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false)
    private String actor;

    private String target;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "previous_hash", columnDefinition = "TEXT", nullable = false)
    private String previousHash;

    @Column(name = "hash", columnDefinition = "TEXT", nullable = false)
    private String hash;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}