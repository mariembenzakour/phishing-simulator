package com.intellisec.phishsim.auth;

import com.intellisec.phishsim.common.util.EncryptionUtil;
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

    // ✅ Le secret TOTP est stocké chiffré en base
    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(nullable = false)
    private String role;

    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;

    private LocalDateTime createdAt;

    // ✅ Pour le chiffrement/déchiffrement
    private static EncryptionUtil encryptionUtil;

    /**
     * ✅ Déchiffrer le secret TOTP à l'utilisation
     */
    public String getDecryptedTotpSecret() {
        if (totpSecret == null || totpSecret.isEmpty()) {
            return null;
        }
        try {
            if (encryptionUtil == null) {
                encryptionUtil = new EncryptionUtil();
            }
            return encryptionUtil.decrypt(totpSecret);
        } catch (Exception e) {
            // Fallback: si le secret n'est pas chiffré (anciennes données)
            return totpSecret;
        }
    }

    /**
     * ✅ Chiffrer le secret TOTP avant stockage
     */
    public void setEncryptedTotpSecret(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            this.totpSecret = null;
            return;
        }
        try {
            if (encryptionUtil == null) {
                encryptionUtil = new EncryptionUtil();
            }
            this.totpSecret = encryptionUtil.encrypt(plainText);
        } catch (Exception e) {
            // Fallback: stocker en clair si le chiffrement échoue
            this.totpSecret = plainText;
        }
    }

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