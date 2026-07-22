package com.intellisec.phishsim.auth;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "operators")
@Data
public class Operator {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String totpSecret;

    @Column(nullable = false)
    private String role;

    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;

    private LocalDateTime createdAt;

    // ✅ Constantes pour les rôles
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_OPERATOR = "OPERATOR";
    public static final String ROLE_VIEWER = "VIEWER";

    // ✅ Méthodes utilitaires pour vérifier les rôles
    public boolean isSuperAdmin() {
        return ROLE_SUPER_ADMIN.equals(this.role);
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(this.role);
    }

    public boolean isAdminOrSuperAdmin() {
        return ROLE_ADMIN.equals(this.role) || ROLE_SUPER_ADMIN.equals(this.role);
    }

    public boolean isOperator() {
        return ROLE_OPERATOR.equals(this.role);
    }
}