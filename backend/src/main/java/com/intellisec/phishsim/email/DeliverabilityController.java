package com.intellisec.phishsim.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/deliverability")
@RequiredArgsConstructor
@Slf4j
public class DeliverabilityController {

    private final DeliverabilityService deliverabilityService;

    /**
     * ✅ Vérification DNS d'un domaine (SPF, DKIM, DMARC)
     */
    @GetMapping("/dns/{domain}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<DeliverabilityService.DomainCheckResult> checkDns(@PathVariable String domain) {
        return ResponseEntity.ok(deliverabilityService.checkDomain(domain));
    }

    /**
     * ✅ Analyse du score de spam via Rspamd
     */
    @PostMapping("/spam")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<DeliverabilityService.SpamCheckResult> checkSpam(@RequestBody Map<String, String> request) {
        String subject = request.getOrDefault("subject", "Test email");
        String bodyHtml = request.getOrDefault("bodyHtml", "<p>This is a test email.</p>");
        String fromEmail = request.getOrDefault("fromEmail", "test@example.com");

        return ResponseEntity.ok(deliverabilityService.checkSpamScore(subject, bodyHtml, fromEmail));
    }

    /**
     * ✅ Rapport complet de délivrabilité (DNS + Spam)
     */
    @PostMapping("/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<DeliverabilityService.DeliverabilityReport> getReport(@RequestBody Map<String, String> request) {
        String domain = request.getOrDefault("domain", "example.com");
        String subject = request.getOrDefault("subject", "Test email");
        String bodyHtml = request.getOrDefault("bodyHtml", "<p>This is a test email.</p>");
        String fromEmail = request.getOrDefault("fromEmail", "test@example.com");

        return ResponseEntity.ok(deliverabilityService.getDeliverabilityReport(domain, subject, bodyHtml, fromEmail));
    }

    /**
     * ✅ Vérification de l'état du service Rspamd
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = Map.of(
                "service", "Rspamd",
                "url", "http://localhost:11333/checkv2",
                "status", "configured",
                "dns", "Google DNS (8.8.8.8)"
        );
        return ResponseEntity.ok(status);
    }
}