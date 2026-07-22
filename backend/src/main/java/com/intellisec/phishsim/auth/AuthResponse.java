package com.intellisec.phishsim.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // ✅ Constructeur sans paramètres
@AllArgsConstructor  // ✅ Constructeur avec tous les paramètres (token, operator, mfaConfigured)
public class AuthResponse {
    private String token;
    private OperatorDTO operator;
    private boolean mfaConfigured;

    // ✅ Constructeur avec 2 paramètres (pour compatibilité)
    public AuthResponse(String token, OperatorDTO operator) {
        this.token = token;
        this.operator = operator;
        this.mfaConfigured = false;  // Valeur par défaut
    }
}