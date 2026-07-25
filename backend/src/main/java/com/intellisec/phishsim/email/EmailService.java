package com.intellisec.phishsim.email;

import com.intellisec.phishsim.campaign.Campaign;
import com.intellisec.phishsim.target.Target;
import com.intellisec.phishsim.target.TargetRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TargetRepository targetRepository;

    /**
     * Envoyer un email à une seule cible
     */
    public void sendEmail(String to, String from, String subject, String body, boolean isHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(body, isHtml);

            mailSender.send(message);
            log.info("✅ Email envoyé à : {}", to);

        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email à {} : {}", to, e.getMessage());
            throw new RuntimeException("Erreur d'envoi d'email", e);
        }
    }

    /**
     * Envoyer une campagne à toutes les cibles d'un groupe
     */
    public void sendCampaign(Campaign campaign, EmailTemplate template) {
        // 1. Récupérer les cibles du groupe
        List<Target> targets = targetRepository.findByGroupId(campaign.getTargetGroupId());

        if (targets.isEmpty()) {
            log.warn("⚠️ Aucune cible trouvée pour la campagne : {}", campaign.getName());
            return;
        }

        // 2. Déterminer l'expéditeur
        String fromEmail = campaign.getSenderProfile() != null
                ? campaign.getSenderProfile().getFromEmail()
                : campaign.getSenderEmail();

        if (fromEmail == null || fromEmail.isEmpty()) {
            throw new RuntimeException("❌ Aucun expéditeur configuré pour la campagne");
        }

        // 3. Mode DRY RUN avec vérification d'appartenance au groupe
        if (Boolean.TRUE.equals(campaign.getDryRun()) && campaign.getDryRunEmail() != null) {
            // ✅ Vérifier que l'email de test appartient bien au groupe
            Target dryRunTarget = targets.stream()
                    .filter(t -> t.getEmail().equalsIgnoreCase(campaign.getDryRunEmail()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "❌ L'email de test " + campaign.getDryRunEmail() +
                                    " n'appartient pas au groupe sélectionné pour cette campagne."
                    ));

            log.info("🔬 DRY RUN : Envoi à {}", campaign.getDryRunEmail());

            // Personnaliser le sujet et le corps avec les données de la cible de test
            String personalizedSubject = personalizeSubject(template.getSubject(), dryRunTarget);
            String personalizedBody = personalizeBody(template.getBodyHtml(), dryRunTarget);

            sendEmail(campaign.getDryRunEmail(), fromEmail, personalizedSubject, personalizedBody, true);
            return;
        }

        // 4. Envoi à toutes les cibles avec throttling et scope-enforcement
        int throttleSeconds = campaign.getThrottleSeconds() != null ? campaign.getThrottleSeconds() : 5;

        for (int i = 0; i < targets.size(); i++) {
            Target target = targets.get(i);
            try {
                // ✅ SCOPE-ENFORCEMENT : Vérifier que la cible appartient au groupe de la campagne
                enforceScope(campaign, target);

                // Personnalisation
                String personalizedSubject = personalizeSubject(template.getSubject(), target);
                String personalizedBody = personalizeBody(template.getBodyHtml(), target);

                sendEmail(target.getEmail(), fromEmail, personalizedSubject, personalizedBody, true);

                // Throttling
                if (i < targets.size() - 1) {
                    Thread.sleep(throttleSeconds * 1000L);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("❌ Throttling interrompu");
                break;
            } catch (Exception e) {
                log.error("❌ Erreur pour {} : {}", target.getEmail(), e.getMessage());
            }
        }

        log.info("✅ Campagne envoyée à {} cibles", targets.size());
    }

    /**
     * ✅ Scope-Enforcement Gate : Vérifie que la cible appartient bien au groupe de la campagne
     * Si ce n'est pas le cas, une exception est levée et l'envoi est bloqué.
     */
    private void enforceScope(Campaign campaign, Target target) {
        if (target.getGroupId() == null || !target.getGroupId().equals(campaign.getTargetGroupId())) {
            String errorMsg = String.format(
                    "❌ Scope Enforcement : La cible %s (groupe %s) n'appartient pas au groupe de la campagne %s (groupe %s)",
                    target.getEmail(),
                    target.getGroupId(),
                    campaign.getName(),
                    campaign.getTargetGroupId()
            );
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * Personnaliser le corps de l'email avec les informations de la cible
     */
    private String personalizeBody(String body, Target target) {
        return body
                .replace("{{firstName}}", target.getFirstName() != null ? target.getFirstName() : "")
                .replace("{{lastName}}", target.getLastName() != null ? target.getLastName() : "")
                .replace("{{email}}", target.getEmail());
    }

    /**
     * Personnaliser le sujet de l'email
     */
    private String personalizeSubject(String subject, Target target) {
        return subject
                .replace("{{firstName}}", target.getFirstName() != null ? target.getFirstName() : "")
                .replace("{{lastName}}", target.getLastName() != null ? target.getLastName() : "");
    }
}