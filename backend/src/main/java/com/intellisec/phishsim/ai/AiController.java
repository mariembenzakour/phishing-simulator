package com.intellisec.phishsim.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiGenerationLogRepository aiGenerationLogRepository;

    /**
     * ✅ Génère un email complet (Week 6)
     * Inclut : email HTML + landing page + red flags
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'SUPER_ADMIN')")
    public ResponseEntity<GenerationResponse> generateEmail(@RequestBody GenerationRequest request) {
        GenerationResponse response = aiService.generateEmail(request);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Approuve un draft généré (Week 6)
     * Met à jour le statut et enregistre qui a approuvé
     */
    @PutMapping("/approve/{generationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GenerationResponse> approve(@PathVariable UUID generationId) {
        AiGenerationLog log = aiGenerationLogRepository.findById(generationId)
                .orElseThrow(() -> new RuntimeException("Génération non trouvée"));

        if (log.getApproved()) {
            throw new RuntimeException("Ce draft a déjà été approuvé");
        }

        log.setApproved(true);
        log.setApprovedBy(getCurrentUser());
        log.setApprovedAt(LocalDateTime.now());
        aiGenerationLogRepository.save(log);

        // ✅ Construction de la réponse avec tous les champs Week 6
        GenerationResponse response = new GenerationResponse();
        response.setId(log.getId().toString());
        response.setSubject(log.getGeneratedSubject());
        response.setBodyHtml(log.getGeneratedBody());
        response.setBodyText(log.getBodyText());
        response.setSenderName(log.getSenderName());
        response.setSenderDomain(log.getSenderDomain());
        response.setLandingPageHtml(log.getLandingPageHtml());
        response.setRedFlags(log.getRedFlags());
        response.setLanguage(log.getLanguage());
        response.setScenario(log.getScenario());
        response.setStatus("APPROVED");
        response.setApproved(true);
        response.setApprovedBy(log.getApprovedBy());
        response.setGeneratedAt(log.getCreatedAt().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Rejette un draft
     */
    @PutMapping("/reject/{generationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> reject(@PathVariable UUID generationId) {
        AiGenerationLog log = aiGenerationLogRepository.findById(generationId)
                .orElseThrow(() -> new RuntimeException("Génération non trouvée"));

        if (log.getApproved()) {
            throw new RuntimeException("Ce draft a déjà été approuvé, vous ne pouvez pas le rejeter");
        }

        log.setApproved(false);
        aiGenerationLogRepository.save(log);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "rejected");
        response.put("message", "Draft rejeté avec succès");
        response.put("generationId", generationId.toString());

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Récupère les drafts NON approuvés (pour la revue)
     */
    @GetMapping("/drafts")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllDrafts() {
        var drafts = aiGenerationLogRepository.findByApproved(false);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", drafts.size());
        response.put("drafts", drafts);

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Récupère les drafts APPROUVÉS (pour utilisation dans les campagnes)
     */
    @GetMapping("/approved")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getApprovedDrafts() {
        var drafts = aiGenerationLogRepository.findByApproved(true);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", drafts.size());
        response.put("drafts", drafts);

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Récupère un draft spécifique
     */
    @GetMapping("/draft/{generationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'SUPER_ADMIN')")
    public ResponseEntity<AiGenerationLog> getDraft(@PathVariable UUID generationId) {
        AiGenerationLog log = aiGenerationLogRepository.findById(generationId)
                .orElseThrow(() -> new RuntimeException("Génération non trouvée"));
        return ResponseEntity.ok(log);
    }

    /**
     * ✅ Vérifie le statut du service IA avec les modèles actifs
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("status", "ready");
        status.put("provider", "Google Gemini");
        status.put("primaryModel", aiService.getPrimaryModel());
        status.put("fallbackModel", aiService.getFallbackModel());
        return ResponseEntity.ok(status);
    }

    /**
     * ✅ Récupère l'utilisateur actuel (pour audit)
     */
    private String getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}