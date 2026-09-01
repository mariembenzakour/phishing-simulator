package com.intellisec.phishsim.campaign;

import com.intellisec.phishsim.ai.AiGenerationLog;
import com.intellisec.phishsim.ai.AiGenerationLogRepository;
import com.intellisec.phishsim.audit.AuditLogService;
import com.intellisec.phishsim.tracking.SendEvent;
import com.intellisec.phishsim.tracking.SendEventRepository;
import com.intellisec.phishsim.tracking.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AuditLogService auditLogService;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final SendEventRepository sendEventRepository;        // ✅ AJOUTÉ
    private final TrackingEventRepository trackingEventRepository; // ✅ AJOUTÉ

    // ── GET ALL ──────────────────────────────────────
    public List<Campaign> getAll() {
        return campaignRepository.findAll();
    }

    // ── GET BY ID ────────────────────────────────────
    public Campaign getById(UUID id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
    }

    // ── CREATE ───────────────────────────────────────
    @Transactional
    public Campaign create(Campaign campaign) {
        campaign.setCreatedAt(LocalDateTime.now());

        if (campaign.getAiGenerationId() != null) {
            loadAiContent(campaign);
        }

        if (campaign.getScheduledAt() != null) {
            campaign.setStatus("SCHEDULED");
        } else {
            campaign.setStatus("DRAFT");
        }

        Campaign saved = campaignRepository.save(campaign);

        auditLogService.logCampaignGeneration(
                "CAMPAIGN_CREATE",
                saved.getId(),
                saved.getName(),
                "Campagne créée en statut: " + saved.getStatus()
        );

        return saved;
    }

    // ── UPDATE ───────────────────────────────────────
    @Transactional
    public Campaign update(UUID id, Campaign campaignData) {
        Campaign campaign = getById(id);

        campaign.setName(campaignData.getName());
        campaign.setTemplateId(campaignData.getTemplateId());
        campaign.setTargetGroupId(campaignData.getTargetGroupId());
        campaign.setSenderEmail(campaignData.getSenderEmail());
        campaign.setScheduledAt(campaignData.getScheduledAt());
        campaign.setSenderProfile(campaignData.getSenderProfile());
        campaign.setDryRun(campaignData.getDryRun());
        campaign.setDryRunEmail(campaignData.getDryRunEmail());
        campaign.setThrottleSeconds(campaignData.getThrottleSeconds());

        if (campaignData.getAiGenerationId() != null &&
                !campaignData.getAiGenerationId().equals(campaign.getAiGenerationId())) {
            campaign.setAiGenerationId(campaignData.getAiGenerationId());
            loadAiContent(campaign);
        }

        Campaign saved = campaignRepository.save(campaign);

        auditLogService.logCampaignGeneration(
                "CAMPAIGN_UPDATE",
                saved.getId(),
                saved.getName(),
                "Campagne mise à jour"
        );

        return saved;
    }

    private void loadAiContent(Campaign campaign) {
        if (campaign.getAiGenerationId() == null) {
            return;
        }

        AiGenerationLog aiLog = aiGenerationLogRepository.findById(campaign.getAiGenerationId())
                .orElseThrow(() -> new RuntimeException("Draft IA non trouvé"));

        if (!aiLog.getApproved()) {
            throw new RuntimeException("Le draft IA n'est pas encore approuvé. Veuillez l'approuver avant de créer la campagne.");
        }

        campaign.setCustomSubject(aiLog.getGeneratedSubject());
        campaign.setCustomBodyHtml(aiLog.getGeneratedBody());
        campaign.setCustomBodyText(aiLog.getBodyText());
    }

    // ── CLONE ────────────────────────────────────────
    @Transactional
    public Campaign clone(UUID id) {
        Campaign original = getById(id);
        Campaign clone = new Campaign();
        clone.setName(original.getName() + " (copie)");
        clone.setSenderEmail(original.getSenderEmail());
        clone.setTemplateId(original.getTemplateId());
        clone.setTargetGroupId(original.getTargetGroupId());
        clone.setCreatedBy(original.getCreatedBy());
        clone.setSenderProfile(original.getSenderProfile());
        clone.setDryRun(original.getDryRun());
        clone.setDryRunEmail(original.getDryRunEmail());
        clone.setThrottleSeconds(original.getThrottleSeconds());

        clone.setAiGenerationId(original.getAiGenerationId());
        clone.setCustomSubject(original.getCustomSubject());
        clone.setCustomBodyHtml(original.getCustomBodyHtml());
        clone.setCustomBodyText(original.getCustomBodyText());

        clone.setStatus("DRAFT");
        clone.setCreatedAt(LocalDateTime.now());
        Campaign saved = campaignRepository.save(clone);

        auditLogService.logCampaignGeneration(
                "CAMPAIGN_CLONE",
                saved.getId(),
                saved.getName(),
                "Cloné depuis: " + original.getId()
        );

        return saved;
    }

    // ── AUTHORIZE ────────────────────────────────────
    @Transactional
    public Campaign authorize(UUID id, UUID operatorId) {
        Campaign campaign = getById(id);
        if (!campaign.getStatus().equals("DRAFT") && !campaign.getStatus().equals("SCHEDULED")) {
            throw new RuntimeException("Only DRAFT or SCHEDULED campaigns can be authorized");
        }
        campaign.setStatus("AUTHORIZED");
        campaign.setAuthorizedBy(operatorId);
        Campaign saved = campaignRepository.save(campaign);

        auditLogService.logAction(
                "CAMPAIGN_AUTHORIZE",
                saved.getId(),
                "Campagne autorisée par: " + operatorId
        );

        return saved;
    }

    // ── PAUSE ────────────────────────────────────────
    @Transactional
    public Campaign pause(UUID id) {
        Campaign campaign = getById(id);
        if (!campaign.getStatus().equals("RUNNING")) {
            throw new RuntimeException("Only RUNNING campaigns can be paused");
        }
        campaign.setStatus("PAUSED");
        Campaign saved = campaignRepository.save(campaign);

        auditLogService.logAction(
                "CAMPAIGN_PAUSE",
                saved.getId(),
                "Campagne mise en pause"
        );

        return saved;
    }

    // ── RESUME ───────────────────────────────────────
    @Transactional
    public Campaign resume(UUID id) {
        Campaign campaign = getById(id);
        if (!campaign.getStatus().equals("PAUSED")) {
            throw new RuntimeException("Only PAUSED campaigns can be resumed");
        }
        campaign.setStatus("RUNNING");
        Campaign saved = campaignRepository.save(campaign);

        auditLogService.logAction(
                "CAMPAIGN_RESUME",
                saved.getId(),
                "Campagne reprise"
        );

        return saved;
    }

    // ── DELETE ───────────────────────────────────────
    @Transactional
    public void delete(UUID id) {
        Campaign campaign = getById(id);

        if (campaign.getStatus().equals("RUNNING")) {
            throw new RuntimeException("Cannot delete a RUNNING campaign");
        }

        // ✅ Supprimer les dépendances : send_events et tracking_events
        List<SendEvent> sendEvents = sendEventRepository.findByCampaignId(id);
        if (!sendEvents.isEmpty()) {
            for (SendEvent se : sendEvents) {
                trackingEventRepository.deleteBySendEventId(se.getId());
            }
            sendEventRepository.deleteAll(sendEvents);
            log.info("🗑️ Supprimés: {} send_events et leurs tracking_events", sendEvents.size());
        }

        auditLogService.logAction(
                "CAMPAIGN_DELETE",
                id,
                "Campagne supprimée: " + campaign.getName()
        );

        campaignRepository.deleteById(id);
        log.info("✅ Campagne '{}' supprimée avec succès", campaign.getName());
    }

    // ── SCHEDULER ────────────────────────────────────
    @Scheduled(fixedRate = 60000)
    public void checkScheduledCampaigns() {
        List<Campaign> scheduled = campaignRepository
                .findByStatusAndScheduledAtBefore("AUTHORIZED", LocalDateTime.now());
        for (Campaign campaign : scheduled) {
            campaign.setStatus("RUNNING");
            campaignRepository.save(campaign);

            auditLogService.logAction(
                    "CAMPAIGN_AUTO_START",
                    campaign.getId(),
                    "Campagne démarrée automatiquement (scheduled)"
            );
        }
    }
}