package com.intellisec.phishsim.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ Configuration centralisée des URLs
 * Permet de changer facilement l'URL de base (localhost, ngrok, Cloudflare, domaine)
 */
@Configuration
public class UrlConfig {

    @Value("${app.base-url:http://localhost:8086}")
    private String baseUrl;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    /**
     * URL de tracking pour un clic
     */
    public String getTrackingClickUrl(String token) {
        return baseUrl + "/track/click/" + token;
    }

    /**
     * URL du pixel de tracking
     */
    public String getTrackingPixelUrl(String token) {
        return baseUrl + "/track/pixel/" + token;
    }

    /**
     * URL de la landing page
     */
    public String getLandingPageUrl(String token) {
        return baseUrl + "/track/landing/" + token;
    }

    /**
     * URL de la page de sensibilisation
     */
    public String getAwarenessPageUrl(String token) {
        return baseUrl + "/track/awareness/" + token;
    }

    /**
     * URL de soumission des credentials
     */
    public String getSubmitUrl(String token) {
        return baseUrl + "/track/submit/" + token;
    }
}