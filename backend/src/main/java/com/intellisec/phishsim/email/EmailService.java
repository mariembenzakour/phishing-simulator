package com.intellisec.phishsim.email;

import com.intellisec.phishsim.campaign.Campaign;
import com.intellisec.phishsim.target.Target;
import com.intellisec.phishsim.target.TargetRepository;
import com.intellisec.phishsim.tracking.SendEvent;
import com.intellisec.phishsim.tracking.TrackingEvent;
import com.intellisec.phishsim.tracking.TrackingService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

    private static final String BASE_URL = "http://localhost:8086";

    private final AtomicInteger emailSentCount = new AtomicInteger(0);
    private final AtomicInteger emailReceivedCount = new AtomicInteger(0);
    private final AtomicInteger totalSent = new AtomicInteger(0);

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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(body, isHtml);

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
            throw new RuntimeException("❌ Aucun expéditeur configuré pour la campagne");
        }

        resetCounters();

        // MODE DRY RUN
        if (Boolean.TRUE.equals(campaign.getDryRun()) && campaign.getDryRunEmail() != null) {

            Target dryRunTarget = targets.stream()
                    .filter(t -> t.getEmail().equalsIgnoreCase(campaign.getDryRunEmail()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "❌ L'email de test " + campaign.getDryRunEmail() +
                                    " n'appartient pas au groupe sélectionné."
                    ));

            log.info("🔬 DRY RUN : Envoi à {}", campaign.getDryRunEmail());

            SendEvent sendEvent = trackingService.createSendEvent(
                    campaign.getId(), dryRunTarget.getId()
            );

            String personalizedSubject = personalizeSubject(template.getSubject(), dryRunTarget);
            String personalizedBody = injectTracking(
                    template.getBodyHtml(), dryRunTarget, sendEvent.getTrackingToken()
            );

            sendEmail(campaign.getDryRunEmail(), fromEmail, personalizedSubject, personalizedBody, true);

            sendEvent.setStatus("SENT");
            trackingService.updateSendEvent(sendEvent);
            log.info("✅ DRY RUN terminé — token : {}", sendEvent.getTrackingToken());

            logDeliverabilityStats(campaign.getName());
            return;
        }

        // ENVOI COMPLET
        int throttleSeconds = campaign.getThrottleSeconds() != null
                ? campaign.getThrottleSeconds() : 5;

        for (int i = 0; i < targets.size(); i++) {
            Target target = targets.get(i);
            try {
                enforceScope(campaign, target);

                SendEvent sendEvent = trackingService.createSendEvent(
                        campaign.getId(), target.getId()
                );

                String personalizedSubject = personalizeSubject(template.getSubject(), target);
                String personalizedBody = injectTracking(
                        template.getBodyHtml(), target, sendEvent.getTrackingToken()
                );

                sendEmail(target.getEmail(), fromEmail, personalizedSubject, personalizedBody, true);

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

        logDeliverabilityStats(campaign.getName());
        log.info("✅ Campagne '{}' envoyée à {} cibles", campaign.getName(), targets.size());
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
            // ✅ Utilisation de TrackingEvent correctement
            List<TrackingEvent> events = trackingService.getEventsBySendEvent(se.getId());
            opens += events.stream().filter(e -> "OPEN".equals(e.getEventType())).count();
            clicks += events.stream().filter(e -> "CLICK".equals(e.getEventType())).count();
            submits += events.stream().filter(e -> "SUBMIT".equals(e.getEventType())).count();
        }

        return new DeliverabilityStats(campaignId, sent, received, opens, clicks, submits);
    }

    // ─────────────────────────────────────────────────
    // SCOPE ENFORCEMENT GATE
    // ─────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────
    // INJECTION TRACKING
    // ─────────────────────────────────────────────────
    private String injectTracking(String body, Target target, String token) {
        String trackingLink = BASE_URL + "/track/click/" + token;

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

        String pixel = "<img src='" + BASE_URL + "/track/pixel/" + token +
                "' width='1' height='1' style='display:none;border:0;' alt=''/>";

        if (body.contains("</body>")) {
            body = body.replace("</body>", pixel + "</body>");
        } else {
            body = body + pixel;
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