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

        // ✅ HSTS
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // ✅ Empêche le MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // ✅ Empêche le clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // ✅ Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // ✅ CSP - Autorise Google Fonts
        response.setHeader("Content-Security-Policy-Report-Only",
                "default-src 'self' https://*.trycloudflare.com https://*.cloudflare.com; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' http://localhost:4200; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                        "font-src 'self' data: https://fonts.gstatic.com; " +
                        "img-src 'self' data: http://localhost:8086 https://*.trycloudflare.com; " +
                        "connect-src 'self' http://localhost:8086 http://localhost:4200 https://*.trycloudflare.com; " +
                        "frame-ancestors 'none'; " +
                        "form-action 'self'");

        // ✅ Cache-Control
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        response.setHeader("Server", "Intellisec-PhishSim");
        response.setHeader("X-Powered-By", "Intellisec");

        filterChain.doFilter(request, response);
    }
}