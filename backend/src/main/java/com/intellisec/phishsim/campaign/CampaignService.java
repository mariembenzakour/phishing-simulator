package com.intellisec.phishsim.campaign;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;

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
    public Campaign create(Campaign campaign) {
        campaign.setCreatedAt(LocalDateTime.now());
        if (campaign.getScheduledAt() != null) {
            campaign.setStatus("SCHEDULED");
        } else {
            campaign.setStatus("DRAFT");
        }
        return campaignRepository.save(campaign);
    }

    // ── CLONE ────────────────────────────────────────
    public Campaign clone(UUID id) {
        Campaign original = getById(id);
        Campaign clone = new Campaign();
        clone.setName(original.getName() + " (copie)");
        clone.setSenderEmail(original.getSenderEmail());
        clone.setTemplateId(original.getTemplateId());
        clone.setTargetGroupId(original.getTargetGroupId());
        clone.setCreatedBy(original.getCreatedBy());
        clone.setStatus("DRAFT");
        clone.setCreatedAt(LocalDateTime.now());
        return campaignRepository.save(clone);
    }

    // ── AUTHORIZE ────────────────────────────────────
    public Campaign authorize(UUID id, UUID operatorId) {
        Campaign campaign = getById(id);
        if (!campaign.getStatus().equals("DRAFT") && !campaign.getStatus().equals("SCHEDULED")) {
            throw new RuntimeException("Only DRAFT or SCHEDULED campaigns can be authorized");
        }
        campaign.setStatus("AUTHORIZED");
        campaign.setAuthorizedBy(operatorId);
        return campaignRepository.save(campaign);
    }

    // ── PAUSE ────────────────────────────────────────
    public Campaign pause(UUID id) {
        Campaign campaign = getById(id);
        if (!campaign.getStatus().equals("RUNNING")) {
            throw new RuntimeException("Only RUNNING campaigns can be paused");
        }
        campaign.setStatus("PAUSED");
        return campaignRepository.save(campaign);
    }

    // ── RESUME ───────────────────────────────────────
    public Campaign resume(UUID id) {
        Campaign campaign = getById(id);
        if (!campaign.getStatus().equals("PAUSED")) {
            throw new RuntimeException("Only PAUSED campaigns can be resumed");
        }
        campaign.setStatus("RUNNING");
        return campaignRepository.save(campaign);
    }

    // ── DELETE ───────────────────────────────────────
    public void delete(UUID id) {
        Campaign campaign = getById(id);
        if (campaign.getStatus().equals("RUNNING")) {
            throw new RuntimeException("Cannot delete a RUNNING campaign");
        }
        campaignRepository.deleteById(id);
    }

    // ── SCHEDULER ────────────────────────────────────
    // Vérifie toutes les minutes si des campagnes SCHEDULED doivent démarrer
    @Scheduled(fixedRate = 60000)
    public void checkScheduledCampaigns() {
        List<Campaign> scheduled = campaignRepository
                .findByStatusAndScheduledAtBefore("AUTHORIZED", LocalDateTime.now());
        for (Campaign campaign : scheduled) {
            campaign.setStatus("RUNNING");
            campaignRepository.save(campaign);
        }
    }
}