package com.intellisec.phishsim.email;

import com.intellisec.phishsim.auth.Operator;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sender_profiles")
@Data
public class SenderProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "from_name", nullable = false)
    private String fromName;

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Column(name = "reply_to")
    private String replyTo;

    @ManyToOne
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private Operator createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}