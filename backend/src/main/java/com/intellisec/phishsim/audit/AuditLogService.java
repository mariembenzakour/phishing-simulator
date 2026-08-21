package com.intellisec.phishsim.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    @Transactional
    public AuditLog log(String action, String target, Object details) {
        List<AuditLog> allLogs = auditLogRepository.findAll();
        String previousHash = allLogs.isEmpty() ? "0" : allLogs.get(allLogs.size() - 1).getHash();

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setActor(getCurrentUser());
        auditLog.setTarget(target);
        auditLog.setPreviousHash(previousHash);

        try {
            auditLog.setDetails(objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            log.warn("⚠️ Erreur sérialisation: {}", e.getMessage());
            auditLog.setDetails("{}");
        }

        String content = action + auditLog.getActor() +
                (target != null ? target : "") +
                auditLog.getDetails() + previousHash;
        auditLog.setHash(hash(content));

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("📝 Audit log: {} par {}", action, auditLog.getActor());
        return saved;
    }

    public AuditLog logAction(String action, String target, String message) {
        return log(action, target, Map.of("message", message));
    }

    public AuditLog logAction(String action, UUID targetId, String message) {
        String target = targetId != null ? targetId.toString() : null;
        return log(action, target, Map.of("message", message));
    }

    @Transactional
    public AuditLog logCampaignSend(UUID campaignId, String campaignName,
                                    int targetCount, boolean dryRun, String dryRunEmail) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("campaignName", campaignName);
        details.put("targetCount", targetCount);
        details.put("dryRun", dryRun);
        if (dryRun && dryRunEmail != null) {
            details.put("dryRunEmail", dryRunEmail);
        }
        String target = campaignId != null ? campaignId.toString() : null;
        return log("CAMPAIGN_SEND", target, details);
    }

    @Transactional
    public AuditLog logCampaignGeneration(String action, UUID campaignId,
                                          String campaignName, String details) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("campaignName", campaignName);
        data.put("details", details);
        String target = campaignId != null ? campaignId.toString() : null;
        return log(action, target, data);
    }

    public Map<String, Object> verifyIntegrity() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<AuditLog> logs = auditLogRepository.findAll();

        if (logs.isEmpty()) {
            result.put("valid", true);
            result.put("message", "✅ Aucun log d'audit (chaîne vide)");
            result.put("totalLogs", 0);
            return result;
        }

        String previousHash = "0";
        boolean valid = true;
        int checked = 0;

        for (AuditLog auditLog : logs) {
            String target = auditLog.getTarget() != null ? auditLog.getTarget() : "";
            String content = auditLog.getAction() + auditLog.getActor() +
                    target + auditLog.getDetails() + previousHash;
            String expectedHash = hash(content);

            if (!expectedHash.equals(auditLog.getHash())) {
                valid = false;
                log.error("❌ Intégrité compromise ! Log: {}", auditLog.getId());
                result.put("firstInvalidLogId", auditLog.getId());
                result.put("firstInvalidAction", auditLog.getAction());
                result.put("firstInvalidActor", auditLog.getActor());
                result.put("firstInvalidCreatedAt", auditLog.getCreatedAt());
                break;
            }
            previousHash = auditLog.getHash();
            checked++;
        }

        result.put("valid", valid);
        result.put("message", valid ? "✅ Chaîne d'audit intègre" : "❌ Intégrité compromise !");
        result.put("totalLogs", logs.size());
        result.put("checked", checked);

        log.info("🔍 Audit integrity: {}", valid ? "OK" : "FAILED");
        return result;
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de hachage", e);
        }
    }

    public long countSends() {
        return auditLogRepository.countTotalSends();
    }

    public long countSendsSince(LocalDateTime since) {
        return auditLogRepository.countSendsSince(since);
    }

    public List<AuditLog> getRecentSends(int limit) {
        return auditLogRepository.findByAction("CAMPAIGN_SEND").stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .toList();
    }

    public List<AuditLog> getLogsByActor(String actor) {
        return auditLogRepository.findByActor(actor);
    }

    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }
}