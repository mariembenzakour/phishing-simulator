package com.intellisec.phishsim.tracking;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tracking_events")
@Data
public class TrackingEvent {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID sendEventId;

    @Column(nullable = false)
    private String eventType; // OPEN / CLICK / SUBMIT

    private LocalDateTime occurredAt;

    private String userAgent;

    private String ipHash;
}