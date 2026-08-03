package com.intellisec.phishsim.tracking;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "send_events")
@Data
public class SendEvent {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID campaignId;
    private UUID targetId;

    @Column(unique = true, nullable = false)
    private String trackingToken;

    private String status; // PENDING / SENT / FAILED

    private LocalDateTime sentAt;

    // ✅ NOUVEAU : Champ pour suivre la délivrabilité
    @Column(name = "delivered")
    private Boolean delivered = false;
}