package com.intellisec.phishsim.auth;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

@Service
public class MfaService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // Génère un secret TOTP pour l'opérateur
    public String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    // Génère le lien QR Code à scanner avec Google Authenticator
    public String generateQrUrl(String email, String secret) {
        return "otpauth://totp/PhishSim:" + email +
                "?secret=" + secret +
                "&issuer=PhishSim";
    }

    // Vérifie si le code TOTP entré est correct
    public boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }
}