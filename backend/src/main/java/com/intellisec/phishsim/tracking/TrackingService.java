package com.intellisec.phishsim.tracking;

import com.intellisec.phishsim.campaign.Campaign;
import com.intellisec.phishsim.campaign.CampaignRepository;
import com.intellisec.phishsim.common.config.UrlConfig;
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
    private final UrlConfig urlConfig;  // ✅ INJECTÉ

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
    // ✅ TIME-TO-CLICK : GLOBAL (Campagne)
    // ============================================================

    /**
     * ✅ Calcule le temps moyen entre l'envoi et le clic pour une campagne
     */
    public double getAverageTimeToClick(UUID campaignId) {
        List<SendEvent> sendEvents = sendEventRepository.findByCampaignId(campaignId);

        if (sendEvents.isEmpty()) {
            return 0.0;
        }

        List<Long> diffs = new ArrayList<>();

        for (SendEvent se : sendEvents) {
            if (se.getSentAt() == null) continue;

            List<TrackingEvent> clicks = trackingEventRepository
                    .findBySendEventIdAndEventType(se.getId(), "CLICK");

            for (TrackingEvent click : clicks) {
                if (click.getOccurredAt() != null) {
                    long diffSeconds = java.time.Duration.between(
                            se.getSentAt(),
                            click.getOccurredAt()
                    ).getSeconds();
                    diffs.add(diffSeconds);
                }
            }
        }

        if (diffs.isEmpty()) {
            return 0.0;
        }

        return diffs.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /**
     * ✅ Statistiques complètes de time-to-click pour une campagne
     * Inclut : moyenne, min, max, médiane, distribution
     */
    public Map<String, Object> getTimeToClickStats(UUID campaignId) {
        List<SendEvent> sendEvents = sendEventRepository.findByCampaignId(campaignId);

        List<Long> diffs = new ArrayList<>();
        Map<String, Long> hourlyDistribution = new HashMap<>();

        for (SendEvent se : sendEvents) {
            if (se.getSentAt() == null) continue;

            List<TrackingEvent> clicks = trackingEventRepository
                    .findBySendEventIdAndEventType(se.getId(), "CLICK");

            for (TrackingEvent click : clicks) {
                if (click.getOccurredAt() != null) {
                    long diffSeconds = java.time.Duration.between(
                            se.getSentAt(),
                            click.getOccurredAt()
                    ).getSeconds();
                    diffs.add(diffSeconds);

                    // Distribution par heure
                    int hour = click.getOccurredAt().getHour();
                    hourlyDistribution.put(String.valueOf(hour),
                            hourlyDistribution.getOrDefault(String.valueOf(hour), 0L) + 1);
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalClicks", (long) diffs.size());

        if (diffs.isEmpty()) {
            stats.put("averageSeconds", 0.0);
            stats.put("averageMinutes", 0.0);
            stats.put("averageHours", 0.0);
            stats.put("minSeconds", 0L);
            stats.put("maxSeconds", 0L);
            stats.put("medianSeconds", 0L);
            stats.put("formatted", "N/A");
            stats.put("distribution", getDistribution(diffs));
            stats.put("hourlyDistribution", new HashMap<>());
            return stats;
        }

        // Statistiques
        long min = diffs.stream().min(Long::compareTo).orElse(0L);
        long max = diffs.stream().max(Long::compareTo).orElse(0L);
        double avg = diffs.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // Médiane
        List<Long> sorted = new ArrayList<>(diffs);
        Collections.sort(sorted);
        long median = sorted.get(sorted.size() / 2);

        stats.put("averageSeconds", avg);
        stats.put("averageMinutes", avg / 60.0);
        stats.put("averageHours", avg / 3600.0);
        stats.put("minSeconds", min);
        stats.put("maxSeconds", max);
        stats.put("medianSeconds", median);
        stats.put("formatted", formatDuration((long) avg));
        stats.put("distribution", getDistribution(diffs));
        stats.put("hourlyDistribution", hourlyDistribution);

        return stats;
    }

    // ============================================================
    // ✅ TIME-TO-CLICK : PAR UTILISATEUR
    // ============================================================

    /**
     * ✅ Calcule le time-to-click moyen pour un utilisateur spécifique
     */
    public double getUserAverageTimeToClick(UUID targetId) {
        List<SendEvent> sendEvents = sendEventRepository.findByTargetId(targetId);

        if (sendEvents.isEmpty()) {
            return 0.0;
        }

        List<Long> diffs = new ArrayList<>();

        for (SendEvent se : sendEvents) {
            if (se.getSentAt() == null) continue;

            List<TrackingEvent> clicks = trackingEventRepository
                    .findBySendEventIdAndEventType(se.getId(), "CLICK");

            for (TrackingEvent click : clicks) {
                if (click.getOccurredAt() != null) {
                    long diffSeconds = java.time.Duration.between(
                            se.getSentAt(),
                            click.getOccurredAt()
                    ).getSeconds();
                    diffs.add(diffSeconds);
                }
            }
        }

        if (diffs.isEmpty()) {
            return 0.0;
        }

        return diffs.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /**
     * ✅ Statistiques de time-to-click pour un utilisateur
     */
    public Map<String, Object> getUserTimeToClickStats(UUID targetId) {
        List<SendEvent> sendEvents = sendEventRepository.findByTargetId(targetId);

        List<Long> diffs = new ArrayList<>();

        for (SendEvent se : sendEvents) {
            if (se.getSentAt() == null) continue;

            List<TrackingEvent> clicks = trackingEventRepository
                    .findBySendEventIdAndEventType(se.getId(), "CLICK");

            for (TrackingEvent click : clicks) {
                if (click.getOccurredAt() != null) {
                    long diffSeconds = java.time.Duration.between(
                            se.getSentAt(),
                            click.getOccurredAt()
                    ).getSeconds();
                    diffs.add(diffSeconds);
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalClicks", (long) diffs.size());

        if (diffs.isEmpty()) {
            stats.put("averageSeconds", 0.0);
            stats.put("averageMinutes", 0.0);
            stats.put("formatted", "N/A");
            stats.put("minSeconds", 0L);
            stats.put("maxSeconds", 0L);
            stats.put("distribution", getDistribution(diffs));
            return stats;
        }

        long min = diffs.stream().min(Long::compareTo).orElse(0L);
        long max = diffs.stream().max(Long::compareTo).orElse(0L);
        double avg = diffs.stream().mapToLong(Long::longValue).average().orElse(0.0);

        stats.put("averageSeconds", avg);
        stats.put("averageMinutes", avg / 60.0);
        stats.put("formatted", formatDuration((long) avg));
        stats.put("minSeconds", min);
        stats.put("maxSeconds", max);
        stats.put("distribution", getDistribution(diffs));

        return stats;
    }

    /**
     * ✅ Obtenir le time-to-click pour tous les utilisateurs d'une campagne
     */
    public List<Map<String, Object>> getUsersTimeToClick(UUID campaignId) {
        List<SendEvent> sendEvents = sendEventRepository.findByCampaignId(campaignId);

        Map<UUID, List<Long>> userClicks = new HashMap<>();

        for (SendEvent se : sendEvents) {
            if (se.getSentAt() == null) continue;

            List<TrackingEvent> clicks = trackingEventRepository
                    .findBySendEventIdAndEventType(se.getId(), "CLICK");

            for (TrackingEvent click : clicks) {
                if (click.getOccurredAt() != null) {
                    long diffSeconds = java.time.Duration.between(
                            se.getSentAt(),
                            click.getOccurredAt()
                    ).getSeconds();

                    userClicks.computeIfAbsent(se.getTargetId(), k -> new ArrayList<>()).add(diffSeconds);
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<UUID, List<Long>> entry : userClicks.entrySet()) {
            List<Long> diffs = entry.getValue();
            double avg = diffs.stream().mapToLong(Long::longValue).average().orElse(0.0);

            Map<String, Object> userStat = new LinkedHashMap<>();
            userStat.put("targetId", entry.getKey());
            userStat.put("averageSeconds", avg);
            userStat.put("averageMinutes", avg / 60.0);
            userStat.put("formatted", formatDuration((long) avg));
            userStat.put("totalClicks", diffs.size());
            userStat.put("minSeconds", diffs.stream().min(Long::compareTo).orElse(0L));
            userStat.put("maxSeconds", diffs.stream().max(Long::compareTo).orElse(0L));

            result.add(userStat);
        }

        // Trier par temps moyen (du plus rapide au plus lent)
        result.sort(Comparator.comparingDouble(o -> (double) o.get("averageSeconds")));

        return result;
    }

    // ============================================================
    // ✅ HELPERS
    // ============================================================

    /**
     * ✅ Calcule la distribution des temps en tranches
     */
    private Map<String, Double> getDistribution(List<Long> diffs) {
        Map<String, Double> dist = new LinkedHashMap<>();

        if (diffs.isEmpty()) {
            dist.put("< 1 min", 0.0);
            dist.put("1-5 min", 0.0);
            dist.put("5-15 min", 0.0);
            dist.put("15-30 min", 0.0);
            dist.put("30-60 min", 0.0);
            dist.put("> 1h", 0.0);
            return dist;
        }

        long total = diffs.size();
        long lessThan1Min = diffs.stream().filter(d -> d < 60).count();
        long between1And5Min = diffs.stream().filter(d -> d >= 60 && d < 300).count();
        long between5And15Min = diffs.stream().filter(d -> d >= 300 && d < 900).count();
        long between15And30Min = diffs.stream().filter(d -> d >= 900 && d < 1800).count();
        long between30And60Min = diffs.stream().filter(d -> d >= 1800 && d < 3600).count();
        long moreThan1H = diffs.stream().filter(d -> d >= 3600).count();

        dist.put("< 1 min", (lessThan1Min * 100.0) / total);
        dist.put("1-5 min", (between1And5Min * 100.0) / total);
        dist.put("5-15 min", (between5And15Min * 100.0) / total);
        dist.put("15-30 min", (between15And30Min * 100.0) / total);
        dist.put("30-60 min", (between30And60Min * 100.0) / total);
        dist.put("> 1h", (moreThan1H * 100.0) / total);

        return dist;
    }

    /**
     * ✅ Formate une durée en secondes en chaîne lisible
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m " + remainingSeconds + "s";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (hours < 24) {
            return hours + "h " + remainingMinutes + "m";
        }
        long days = hours / 24;
        long remainingHours = hours % 24;
        return days + "j " + remainingHours + "h";
    }

    // ============================================================
    // EXISTANT : Dashboard
    // ============================================================

    public List<SendEvent> getAllSendEvents() {
        return sendEventRepository.findAll();
    }

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    public long countEventsByType(String eventType) {
        return trackingEventRepository.findByEventType(eventType).size();
    }

    public long getTotalTargets() {
        return targetRepository.count();
    }

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
        stats.put("received", total);
        stats.put("opens", opens);
        stats.put("clicks", clicks);
        stats.put("submits", submits);
        stats.put("openRate", total > 0 ? (opens * 100.0 / total) : 0);
        stats.put("clickRate", total > 0 ? (clicks * 100.0 / total) : 0);
        stats.put("submitRate", total > 0 ? (submits * 100.0 / total) : 0);
        stats.put("sendEvents", sendEvents);

        return stats;
    }

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

    public List<TrackingEvent> getUserHistory(UUID targetId) {
        return trackingEventRepository.findUserHistory(targetId);
    }

    public Campaign getCampaignById(UUID campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
    }
}