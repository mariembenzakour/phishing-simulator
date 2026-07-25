package com.intellisec.phishsim.campaign;

import com.intellisec.phishsim.email.SenderProfile;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Data
public class Campaign {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    private UUID templateId;
    private UUID targetGroupId;
    private String senderEmail;

    @Column(nullable = false)
    private String status; // DRAFT / SCHEDULED / AUTHORIZED / RUNNING / PAUSED / COMPLETED

    private UUID authorizedBy;
    private UUID createdBy;

    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;

    // ✅ NOUVEAUX CHAMPS POUR L'EMAIL
    @ManyToOne
    @JoinColumn(name = "sender_profile_id", referencedColumnName = "id")
    private SenderProfile senderProfile;

    @Column(name = "dry_run")
    private Boolean dryRun = false;

    @Column(name = "dry_run_email")
    private String dryRunEmail;

    @Column(name = "throttle_seconds")
    private Integer throttleSeconds = 5;
}