package com.intellisec.phishsim.tracking;

import com.intellisec.phishsim.campaign.Campaign;
import com.intellisec.phishsim.campaign.CampaignRepository;
import com.intellisec.phishsim.target.TargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final SendEventRepository sendEventRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final CampaignRepository campaignRepository;
    private final TargetRepository targetRepository;

    // ── EXISTANT : Créer un send event ──
    public SendEvent createSendEvent(UUID campaignId, UUID targetId) {
        SendEvent event = new SendEvent();
        event.setCampaignId(campaignId);
        event.setTargetId(targetId);
        event.setTrackingToken(UUID.randomUUID().toString().replace("-", ""));
        event.setStatus("PENDING");
        event.setSentAt(LocalDateTime.now());
        return sendEventRepository.save(event);
    }

    public void updateSendEvent(SendEvent sendEvent) {
        sendEventRepository.save(sendEvent);
    }

    // ── EXISTANT : Tracking ──
    public void trackOpen(String token, String userAgent, String ipHash) {
        sendEventRepository.findByTrackingToken(token).ifPresent(sendEvent -> {
            List<TrackingEvent> existing = trackingEventRepository
                    .findBySendEventIdAndEventType(sendEvent.getId(), "OPEN");
            if (!existing.isEmpty()) return;

            TrackingEvent event = new TrackingEvent();
            event.setSendEventId(sendEvent.getId());
            event.setEventType("OPEN");
            event.setOccurredAt(LocalDateTime.now());
            event.setUserAgent(userAgent);
            event.setIpHash(ipHash);
            trackingEventRepository.save(event);
            log.info("✅ OPEN tracked for token: {}", token);
        });
    }

    public void trackClick(String token, String userAgent, String ipHash) {
        sendEventRepository.findByTrackingToken(token).ifPresent(sendEvent -> {
            TrackingEvent event = new TrackingEvent();
            event.setSendEventId(sendEvent.getId());
            event.setEventType("CLICK");
            event.setOccurredAt(LocalDateTime.now());
            event.setUserAgent(userAgent);
            event.setIpHash(ipHash);
            trackingEventRepository.save(event);
            log.info("✅ CLICK tracked for token: {}", token);
        });
    }

    public void trackSubmit(String token, String userAgent, String ipHash) {
        sendEventRepository.findByTrackingToken(token).ifPresent(sendEvent -> {
            TrackingEvent event = new TrackingEvent();
            event.setSendEventId(sendEvent.getId());
            event.setEventType("SUBMIT");
            event.setOccurredAt(LocalDateTime.now());
            event.setUserAgent(userAgent);
            event.setIpHash(ipHash);
            trackingEventRepository.save(event);
            log.info("✅ SUBMIT tracked for token: {} — credentials discarded", token);
        });
    }

    // ── EXISTANT : Timeline ──
    public List<SendEvent> getEventsByCampaign(UUID campaignId) {
        return sendEventRepository.findByCampaignId(campaignId);
    }

    public List<TrackingEvent> getEventsBySendEvent(UUID sendEventId) {
        return trackingEventRepository.findBySendEventId(sendEventId);
    }

    public SendEvent getByToken(String token) {
        return sendEventRepository.findByTrackingToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid tracking token"));
    }

    // ============================================================
    // ✅ NOUVEAUX : POUR LE DASHBOARD
    // ============================================================

    // ── GET ALL SEND EVENTS ──
    public List<SendEvent> getAllSendEvents() {
        return sendEventRepository.findAll();
    }

    // ── GET ALL CAMPAIGNS ──
    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    // ── COUNT EVENTS BY TYPE ──
    public long countEventsByType(String eventType) {
        return trackingEventRepository.findByEventType(eventType).size();
    }

    // ── GET TOTAL TARGETS ──
    public long getTotalTargets() {
        return targetRepository.count();
    }

    // ── STATISTIQUES PAR CAMPAGNE (pour le dashboard) ──
    public Map<String, Object> getCampaignStats(UUID campaignId) {
        List<SendEvent> sendEvents = getEventsByCampaign(campaignId);
        long total = sendEvents.size();
        long opens = 0, clicks = 0, submits = 0;

        for (SendEvent se : sendEvents) {
            List<TrackingEvent> events = getEventsBySendEvent(se.getId());
            for (TrackingEvent te : events) {
                if ("OPEN".equals(te.getEventType())) opens++;
                if ("CLICK".equals(te.getEventType())) clicks++;
                if ("SUBMIT".equals(te.getEventType())) submits++;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("sent", total);
        stats.put("received", total); // Mailpit capte tout
        stats.put("opens", opens);
        stats.put("clicks", clicks);
        stats.put("submits", submits);
        stats.put("openRate", total > 0 ? (opens * 100.0 / total) : 0);
        stats.put("clickRate", total > 0 ? (clicks * 100.0 / total) : 0);
        stats.put("submitRate", total > 0 ? (submits * 100.0 / total) : 0);
        stats.put("sendEvents", sendEvents);

        return stats;
    }

    // ── STATISTIQUES PAR UTILISATEUR ──
    public List<Map<String, Object>> getUserStats() {
        List<Object[]> results = trackingEventRepository.getUserStats();
        List<Map<String, Object>> stats = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> userStat = new LinkedHashMap<>();
            userStat.put("targetId", row[0] != null ? row[0].toString() : null);
            userStat.put("email", row[1] != null ? row[1].toString() : "");
            userStat.put("firstName", row[2] != null ? row[2].toString() : "");
            userStat.put("lastName", row[3] != null ? row[3].toString() : "");
            userStat.put("totalOpens", row[4] != null ? ((Number) row[4]).longValue() : 0L);
            userStat.put("totalClicks", row[5] != null ? ((Number) row[5]).longValue() : 0L);
            userStat.put("totalSubmits", row[6] != null ? ((Number) row[6]).longValue() : 0L);
            stats.add(userStat);
        }
        return stats;
    }

    // ── HISTORIQUE D'UN UTILISATEUR ──
    public List<TrackingEvent> getUserHistory(UUID targetId) {
        return trackingEventRepository.findUserHistory(targetId);
    }

    // ── RÉCUPÉRER UNE CAMPAGNE PAR SON ID ──
    public Campaign getCampaignById(UUID campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
    }
}