package com.intellisec.phishsim.campaign;

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
}