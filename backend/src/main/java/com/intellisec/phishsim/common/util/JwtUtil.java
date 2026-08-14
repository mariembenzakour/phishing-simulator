package com.intellisec.phishsim.common.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {

    private final String SECRET = "phishsim-secret-key-intellisec-2026-very-long";
    private final long EXPIRATION = 86400000; // 24h

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // ✅ Génération avec ID
    public String generateToken(String email, String role, UUID id) {
        String token = Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("id", id != null ? id.toString() : null)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();

        log.debug("🔑 Token généré pour {}: {}...", email, token.substring(0, Math.min(20, token.length())));
        return token;
    }

    // ✅ Compatibilité ancienne méthode (si utilisée ailleurs)
    public String generateToken(String email, String role) {
        return generateToken(email, role, null);
    }

    public String extractEmail(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            log.error("❌ Erreur extraction email: {}", e.getMessage());
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("role", String.class);
        } catch (Exception e) {
            log.error("❌ Erreur extraction role: {}", e.getMessage());
            return null;
        }
    }

    // ✅ Nouvelle méthode pour extraire l'ID
    public String extractId(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("id", String.class);
        } catch (Exception e) {
            log.error("❌ Erreur extraction id: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("❌ Token invalide: {}", e.getMessage());
            return false;
        }
    }
}