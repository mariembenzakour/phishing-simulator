package com.intellisec.phishsim.email;

import com.intellisec.phishsim.campaign.Campaign;
import com.intellisec.phishsim.campaign.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

        // ✅ Récupérer ou créer un template par défaut
        EmailTemplate template;
        if (campaign.getTemplateId() != null) {
            template = emailRepository.findById(campaign.getTemplateId()).orElse(null);
        } else {
            template = null;
        }

        if (template == null) {
            // ✅ Créer un template par défaut
            template = new EmailTemplate();
            template.setName("Template par défaut - " + campaign.getName());
            template.setSubject("Test de phishing - " + campaign.getName());
            template.setBodyHtml(
                    "<h1>Phishing Simulation</h1>" +
                            "<p>Bonjour {{firstName}},</p>" +
                            "<p>Ceci est un test de simulation de phishing.</p>" +
                            "<p>Cliquez sur le lien pour vous connecter : " +
                            "<a href='http://localhost:4200/login'>Connexion</a></p>" +
                            "<p>L'équipe Intellisec</p>"
            );
            template.setBodyText("Phishing Simulation. Bonjour. Ceci est un test.");
            template.setIsHtml(true);
            template.setStatus("APPROVED");
            template = emailRepository.save(template);

            // ✅ Mettre à jour la campagne avec le templateId
            campaign.setTemplateId(template.getId());
            campaignService.update(campaignId, campaign);
        }

        if (!"APPROVED".equals(template.getStatus())) {
            return ResponseEntity.badRequest().body("❌ Le template doit être approuvé avant envoi");
        }

        emailService.sendCampaign(campaign, template);
        campaign.setStatus("RUNNING");
        campaignService.update(campaignId, campaign);

        return ResponseEntity.ok("✅ Campagne envoyée avec succès !");
    }
}