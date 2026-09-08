package com.intellisec.phishsim.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellisec.phishsim.audit.AuditLogService;
import com.intellisec.phishsim.campaign.Campaign;
import com.intellisec.phishsim.common.config.UrlConfig;
import com.intellisec.phishsim.target.Target;
import com.intellisec.phishsim.target.TargetRepository;
import com.intellisec.phishsim.tracking.SendEvent;
import com.intellisec.phishsim.tracking.TrackingEvent;
import com.intellisec.phishsim.tracking.TrackingService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TargetRepository targetRepository;
    private final TrackingService trackingService;
    private final UrlConfig urlConfig;
    private final AuditLogService auditLogService;

    @Value("${app.email.sender:test@intellisec.com}")
    private String defaultSender;

    @Value("${app.email.sender-name:PhishSim Test}")
    private String defaultSenderName;

    private final AtomicInteger emailSentCount = new AtomicInteger(0);
    private final AtomicInteger emailReceivedCount = new AtomicInteger(0);
    private final AtomicInteger totalSent = new AtomicInteger(0);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public int getEmailSentCount() {
        return emailSentCount.get();
    }

    public int getEmailReceivedCount() {
        return emailReceivedCount.get();
    }

    public int getTotalSent() {
        return totalSent.get();
    }

    public void resetCounters() {
        emailSentCount.set(0);
        emailReceivedCount.set(0);
        log.info("🔄 Compteurs réinitialisés pour la nouvelle campagne");
    }

    // ─────────────────────────────────────────────────
    // ENVOI D'UN SEUL EMAIL
    // ─────────────────────────────────────────────────
    public void sendEmail(String to, String from, String subject, String body, boolean isHtml) {
        sendEmail(to, from, defaultSenderName, subject, body, isHtml);
    }

    public void sendEmail(String to, String from, String fromName, String subject, String body, boolean isHtml) {
        try {
            // ✅ Activer la prise en charge UTF-8 globale dans JavaMail
            System.setProperty("mail.mime.allowutf8", "true");

            MimeMessage message = mailSender.createMimeMessage();

            // ✅ Utilisation du mode MIXED_RELATED avec encodage UTF-8 explicite
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);

            if (fromName != null && !fromName.isEmpty()) {
                try {
                    InternetAddress fromAddress = new InternetAddress(from, fromName, StandardCharsets.UTF_8.name());
                    helper.setFrom(fromAddress);
                } catch (UnsupportedEncodingException e) {
                    log.warn("⚠️ Erreur d'encodage du nom '{}', envoi sans nom", fromName);
                    helper.setFrom(from);
                }
            } else {
                helper.setFrom(from);
            }

            helper.setSubject(subject);

            // ✅ Force l'encodage UTF-8 et la génération du header Content-Type: text/html; charset=UTF-8
            helper.setText(body, isHtml);

            // ✅ Forcer la finalisation des en-têtes et des boundaries MIME avant envoi
            message.saveChanges();

            mailSender.send(message);

            int sent = emailSentCount.incrementAndGet();
            int received = emailReceivedCount.incrementAndGet();
            int total = totalSent.incrementAndGet();

            log.info("✅ Email envoyé à : {} (Sent: {}, Received: {}, Total: {})",
                    to, sent, received, total);

        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email à {} : {}", to, e.getMessage());
            throw new RuntimeException("Erreur d'envoi d'email", e);
        }
    }

    // ─────────────────────────────────────────────────
    // ENVOI D'UNE CAMPAGNE COMPLÈTE
    // ─────────────────────────────────────────────────
    public void sendCampaign(Campaign campaign, EmailTemplate template) {

        List<Target> targets = targetRepository.findByGroupId(campaign.getTargetGroupId());

        if (targets.isEmpty()) {
            log.warn("⚠️ Aucune cible trouvée pour la campagne : {}", campaign.getName());
            return;
        }

        String fromEmail = campaign.getSenderProfile() != null
                ? campaign.getSenderProfile().getFromEmail()
                : campaign.getSenderEmail();

        if (fromEmail == null || fromEmail.isEmpty()) {
            fromEmail = defaultSender;
        }

        String fromName = campaign.getSenderProfile() != null
                ? campaign.getSenderProfile().getFromName()
                : defaultSenderName;

        resetCounters();

        // ✅ DÉTERMINER LE CONTENU DE L'EMAIL
        String subject;
        String bodyHtml;
        String bodyText;

        if (campaign.getCustomSubject() != null && !campaign.getCustomSubject().isEmpty()) {
            subject = campaign.getCustomSubject();
            bodyHtml = campaign.getCustomBodyHtml() != null ? campaign.getCustomBodyHtml() : "";
            bodyText = campaign.getCustomBodyText() != null ? campaign.getCustomBodyText() : "";
            log.info("📧 Utilisation du contenu IA (draft: {})", campaign.getAiGenerationId());
        } else if (template != null && template.getBodyHtml() != null && !template.getBodyHtml().isEmpty()) {
            subject = template.getSubject();
            bodyHtml = template.getBodyHtml();
            bodyText = template.getBodyText() != null ? template.getBodyText() : "";
            log.info("📧 Utilisation du template: {}", template.getName());
        } else {
            throw new RuntimeException(
                    "❌ Aucun contenu d'email défini pour la campagne '" + campaign.getName() + "'. " +
                            "Veuillez soit :\n" +
                            "  1. Associer un draft IA approuvé (recommandé)\n" +
                            "  2. Utiliser un template email existant\n" +
                            "  3. Définir un contenu personnalisé dans la campagne"
            );
        }

        // MODE DRY RUN
        if (Boolean.TRUE.equals(campaign.getDryRun()) && campaign.getDryRunEmail() != null) {

            Target dryRunTarget = targets.stream()
                    .filter(t -> t.getEmail().equalsIgnoreCase(campaign.getDryRunEmail()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "❌ L'email de test " + campaign.getDryRunEmail() +
                                    " n'appartient pas au groupe sélectionné."
                    ));

            // ✅ Scope Enforcement pour le Dry Run
            enforceScope(campaign, dryRunTarget);

            log.info("🔬 DRY RUN : Envoi à {}", campaign.getDryRunEmail());

            SendEvent sendEvent = trackingService.createSendEvent(
                    campaign.getId(), dryRunTarget.getId()
            );

            String personalizedSubject = personalizeSubject(subject, dryRunTarget);
            String personalizedBody = injectTracking(bodyHtml, dryRunTarget, sendEvent.getTrackingToken());

            sendEmail(campaign.getDryRunEmail(), fromEmail, fromName, personalizedSubject, personalizedBody, true);

            sendEvent.setStatus("SENT");
            trackingService.updateSendEvent(sendEvent);
            log.info("✅ DRY RUN terminé — token : {}", sendEvent.getTrackingToken());

            auditLogService.logCampaignSend(
                    campaign.getId(),
                    campaign.getName(),
                    1,
                    true,
                    campaign.getDryRunEmail()
            );

            logDeliverabilityStats(campaign.getName());
            return;
        }

        // ENVOI COMPLET
        int throttleSeconds = campaign.getThrottleSeconds() != null
                ? campaign.getThrottleSeconds() : 5;

        for (int i = 0; i < targets.size(); i++) {
            Target target = targets.get(i);
            try {
                // ✅ Scope Enforcement avec Allow-List
                enforceScope(campaign, target);

                SendEvent sendEvent = trackingService.createSendEvent(
                        campaign.getId(), target.getId()
                );

                String personalizedSubject = personalizeSubject(subject, target);
                String personalizedBody = injectTracking(bodyHtml, target, sendEvent.getTrackingToken());

                sendEmail(target.getEmail(), fromEmail, fromName, personalizedSubject, personalizedBody, true);

                sendEvent.setStatus("SENT");
                trackingService.updateSendEvent(sendEvent);
                log.info("✅ Mail envoyé à {} — token : {}", target.getEmail(), sendEvent.getTrackingToken());

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

        auditLogService.logCampaignSend(
                campaign.getId(),
                campaign.getName(),
                targets.size(),
                false,
                null
        );

        logDeliverabilityStats(campaign.getName());
        log.info("✅ Campagne '{}' envoyée à {} cibles", campaign.getName(), targets.size());
    }

    // ─────────────────────────────────────────────────
    // SCOPE ENFORCEMENT GATE AVEC ALLOW-LIST
    // ─────────────────────────────────────────────────
    private void enforceScope(Campaign campaign, Target target) {
        boolean isInGroup = target.getGroupId() != null
                && target.getGroupId().equals(campaign.getTargetGroupId());

        boolean isInAllowList = false;
        if (campaign.getAllowList() != null && !campaign.getAllowList().isEmpty()) {
            try {
                List<String> allowList = objectMapper.readValue(campaign.getAllowList(), List.class);
                isInAllowList = allowList.stream()
                        .anyMatch(email -> email.equalsIgnoreCase(target.getEmail()));
            } catch (Exception e) {
                log.warn("⚠️ Erreur de parsing de l'allow-list: {}", e.getMessage());
            }
        }

        if (!isInGroup && !isInAllowList) {
            String errorMsg = String.format(
                    "❌ Scope Enforcement : La cible %s (groupe %s) n'est pas autorisée. " +
                            "Elle n'appartient pas au groupe de la campagne %s (groupe %s) " +
                            "et n'est pas dans l'allow-list.",
                    target.getEmail(),
                    target.getGroupId(),
                    campaign.getName(),
                    campaign.getTargetGroupId()
            );
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        log.debug("✅ Cible {} autorisée (groupe: {}, allow-list: {})",
                target.getEmail(), isInGroup, isInAllowList);
    }

    // ─────────────────────────────────────────────────
    // STATISTIQUES DE DÉLIVRABILITÉ
    // ─────────────────────────────────────────────────
    private void logDeliverabilityStats(String campaignName) {
        int sent = emailSentCount.get();
        int received = emailReceivedCount.get();

        log.info("═══════════════════════════════════════════════");
        log.info("📊 STATISTIQUES DE DÉLIVRABILITÉ");
        log.info("   Campagne : {}", campaignName);
        log.info("   📤 Emails envoyés : {}", sent);
        log.info("   📥 Emails reçus   : {}", received);
        log.info("   📈 Taux de délivrabilité : {}%",
                sent > 0 ? (received * 100 / sent) : 0);
        log.info("   📊 Total depuis le démarrage : {}", totalSent.get());
        log.info("═══════════════════════════════════════════════");
    }

    // ─────────────────────────────────────────────────
    // GET STATS POUR UNE CAMPAGNE SPÉCIFIQUE
    // ─────────────────────────────────────────────────
    public DeliverabilityStats getDeliverabilityStats(UUID campaignId) {
        List<SendEvent> sendEvents = trackingService.getEventsByCampaign(campaignId);

        long sent = sendEvents.stream()
                .filter(e -> "SENT".equals(e.getStatus()))
                .count();

        long received = sent;

        long opens = 0, clicks = 0, submits = 0;
        for (SendEvent se : sendEvents) {
            List<TrackingEvent> events = trackingService.getEventsBySendEvent(se.getId());
            opens += events.stream().filter(e -> "OPEN".equals(e.getEventType())).count();
            clicks += events.stream().filter(e -> "CLICK".equals(e.getEventType())).count();
            submits += events.stream().filter(e -> "SUBMIT".equals(e.getEventType())).count();
        }

        return new DeliverabilityStats(campaignId, sent, received, opens, clicks, submits);
    }

    // ─────────────────────────────────────────────────
    // INJECTION TRACKING & GARANTIE UTF-8
    // ─────────────────────────────────────────────────
    private String injectTracking(String body, Target target, String token) {
        String trackingLink = urlConfig.getTrackingClickUrl(token);
        String pixelUrl = urlConfig.getTrackingPixelUrl(token);

        body = body
                .replace("{{firstName}}", target.getFirstName() != null ? target.getFirstName() : "")
                .replace("{{lastName}}", target.getLastName() != null ? target.getLastName() : "")
                .replace("{{email}}", target.getEmail())
                .replace("TRACKING_LINK", trackingLink);

        body = body
                .replace("href='http://localhost:4200/login'",
                        "href='" + trackingLink + "'")
                .replace("href=\"http://localhost:4200/login\"",
                        "href=\"" + trackingLink + "\"");

        String pixel = "<img src='" + pixelUrl +
                "' width='1' height='1' style='display:none;border:0;' alt=''/>";

        if (body.contains("</body>")) {
            body = body.replace("</body>", pixel + "</body>");
        } else {
            body = body + pixel;
        }

        // ✅ Injecter la balise meta UTF-8 si absente pour forcer le rendu correct des caractères accentués
        if (body.toLowerCase().contains("<head>") && !body.toLowerCase().contains("charset=")) {
            body = body.replaceFirst("(?i)<head>", "<head><meta charset=\"UTF-8\">");
        } else if (!body.toLowerCase().contains("charset=")) {
            body = "<meta charset=\"UTF-8\">" + body;
        }

        return body;
    }

    private String personalizeSubject(String subject, Target target) {
        return subject
                .replace("{{firstName}}", target.getFirstName() != null ? target.getFirstName() : "")
                .replace("{{lastName}}", target.getLastName() != null ? target.getLastName() : "");
    }

    // ─────────────────────────────────────────────────
    // STATS DTO
    // ─────────────────────────────────────────────────
    public static class DeliverabilityStats {
        public final UUID campaignId;
        public final long sent;
        public final long received;
        public final long opens;
        public final long clicks;
        public final long submits;

        public DeliverabilityStats(UUID campaignId, long sent, long received,
                                   long opens, long clicks, long submits) {
            this.campaignId = campaignId;
            this.sent = sent;
            this.received = received;
            this.opens = opens;
            this.clicks = clicks;
            this.submits = submits;
        }

        public long getSent() { return sent; }
        public long getReceived() { return received; }
        public long getOpens() { return opens; }
        public long getClicks() { return clicks; }
        public long getSubmits() { return submits; }
        public double getOpenRate() {
            return sent > 0 ? (opens * 100.0 / sent) : 0;
        }
        public double getClickRate() {
            return sent > 0 ? (clicks * 100.0 / sent) : 0;
        }
        public double getSubmitRate() {
            return sent > 0 ? (submits * 100.0 / sent) : 0;
        }
    }
}