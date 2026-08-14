package com.intellisec.phishsim.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersConfig extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ✅ HSTS - Actif pour HTTPS
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // ✅ Empêche le MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // ✅ Empêche le clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // ✅ Contrôle la politique de referrer
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // ✅ CSP - Permettre les ressources externes
        response.setHeader("Content-Security-Policy-Report-Only",
                "default-src 'self' https://*.trycloudflare.com; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' http://localhost:4200; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: http://localhost:8086; " +
                        "connect-src 'self' http://localhost:8086 http://localhost:4200 https://*.trycloudflare.com; " +
                        "font-src 'self' data:; " +
                        "frame-ancestors 'none'; " +
                        "form-action 'self'");

        // ✅ Cache-Control
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // ✅ Server header
        response.setHeader("Server", "Intellisec-PhishSim");
        response.setHeader("X-Powered-By", "Intellisec");

        filterChain.doFilter(request, response);
    }
}