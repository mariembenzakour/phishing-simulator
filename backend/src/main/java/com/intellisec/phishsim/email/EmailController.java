package com.intellisec.phishsim.email;

import com.intellisec.phishsim.campaign.Campaign;
import com.intellisec.phishsim.campaign.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final CampaignService campaignService;
    private final EmailRepository emailRepository;

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<String> sendTestEmail(@RequestBody Map<String, String> request) {
        String to = request.get("to");
        String subject = request.get("subject");
        String body = request.get("body");
        emailService.sendEmail(to, "test@intellisec.com", subject, body, true);
        return ResponseEntity.ok("✅ Email envoyé à " + to);
    }

    @PostMapping("/send-campaign/{campaignId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<String> sendCampaign(@PathVariable UUID campaignId) {
        Campaign campaign = campaignService.getById(campaignId);

        if (!"AUTHORIZED".equals(campaign.getStatus()) && !"SCHEDULED".equals(campaign.getStatus())) {
            return ResponseEntity.badRequest().body("❌ La campagne doit être autorisée avant d'être envoyée");
        }

        // ✅ Récupérer le template si associé
        EmailTemplate template = null;
        if (campaign.getTemplateId() != null) {
            template = emailRepository.findById(campaign.getTemplateId()).orElse(null);
        }

        // ✅ Le template par défaut est géré dans EmailService
        // Plus de code en dur ici ! Le contenu IA est prioritaire.

        if (template != null && !"APPROVED".equals(template.getStatus())) {
            return ResponseEntity.badRequest().body("❌ Le template doit être approuvé avant envoi");
        }

        emailService.sendCampaign(campaign, template);
        campaign.setStatus("RUNNING");
        campaignService.update(campaignId, campaign);

        return ResponseEntity.ok("✅ Campagne envoyée avec succès !");
    }

    @GetMapping("/stats/{campaignId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> getDeliverabilityStats(@PathVariable UUID campaignId) {
        Campaign campaign = campaignService.getById(campaignId);
        EmailService.DeliverabilityStats stats = emailService.getDeliverabilityStats(campaignId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("campaignId", campaignId.toString());
        response.put("campaignName", campaign.getName());
        response.put("sent", stats.getSent());
        response.put("received", stats.getReceived());
        response.put("opens", stats.getOpens());
        response.put("clicks", stats.getClicks());
        response.put("submits", stats.getSubmits());
        response.put("openRate", stats.getOpenRate());
        response.put("clickRate", stats.getClickRate());
        response.put("submitRate", stats.getSubmitRate());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/global")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalEmailsSent", emailService.getTotalSent());
        return ResponseEntity.ok(response);
    }
}