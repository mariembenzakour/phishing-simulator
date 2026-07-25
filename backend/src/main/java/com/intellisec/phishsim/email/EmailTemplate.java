package com.intellisec.phishsim.email;

import com.intellisec.phishsim.auth.Operator;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_templates")
@Data
public class EmailTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subject;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    private String status; // DRAFT, APPROVED

    @ManyToOne
    @JoinColumn(name = "approved_by", referencedColumnName = "id")
    private Operator approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ✅ NOUVEAU CHAMP POUR LE TEXTE BRUT
    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "is_html")
    private Boolean isHtml = true;
}