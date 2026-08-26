package com.intellisec.phishsim.ai;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_generation_logs")
@Data
public class AiGenerationLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String scenario;

    private String language;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String generatedSubject;

    @Column(columnDefinition = "TEXT")
    private String generatedBody;

    @Column(columnDefinition = "TEXT")
    private String bodyText;

    @Column(columnDefinition = "TEXT")
    private String landingPageHtml;

    // ✅ NOUVEAU : Champ séparé pour l'awareness page
    @Column(columnDefinition = "TEXT")
    private String awarenessPageHtml;

    @Column(columnDefinition = "TEXT")
    private String redFlags;

    @Column(columnDefinition = "TEXT")
    private String senderName;

    @Column(columnDefinition = "TEXT")
    private String senderDomain;

    @Column(nullable = false)
    private String generatedBy;

    private String approvedBy;

    private Boolean approved = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;
}