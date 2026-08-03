package com.intellisec.phishsim.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final SendEventRepository sendEventRepository;
    private final TrackingEventRepository trackingEventRepository;

    // ── Créer un send event pour chaque cible ────────
    public SendEvent createSendEvent(UUID campaignId, UUID targetId) {
        SendEvent event = new SendEvent();
        event.setCampaignId(campaignId);
        event.setTargetId(targetId);
        event.setTrackingToken(UUID.randomUUID().toString().replace("-", ""));
        event.setStatus("PENDING");
        event.setSentAt(LocalDateTime.now());
        return sendEventRepository.save(event);
    }

    // ✅ NOUVEAU : Mettre à jour un send event
    public void updateSendEvent(SendEvent sendEvent) {
        sendEventRepository.save(sendEvent);
    }

    // ── Enregistrer un événement OPEN ─────────────────
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

    // ── Enregistrer un événement CLICK ────────────────
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

    // ── Enregistrer un événement SUBMIT ──────────────
    // ⚠️ GUARDRAIL : on ne stocke JAMAIS les valeurs soumises
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

    // ── Timeline par campagne ─────────────────────────
    public List<SendEvent> getEventsByCampaign(UUID campaignId) {
        return sendEventRepository.findByCampaignId(campaignId);
    }

    // ── Timeline par cible ────────────────────────────
    public List<TrackingEvent> getEventsBySendEvent(UUID sendEventId) {
        return trackingEventRepository.findBySendEventId(sendEventId);
    }

    // ── Vérifier token valide ─────────────────────────
    public SendEvent getByToken(String token) {
        return sendEventRepository.findByTrackingToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid tracking token"));
    }
}