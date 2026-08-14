package com.intellisec.phishsim.common.util;

import com.intellisec.phishsim.tracking.SendEventRepository;
import com.intellisec.phishsim.tracking.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ✅ Service de politique de rétention des données
 * Nettoie automatiquement les données anciennes pour respecter
 * la politique de confidentialité
 *
 * Emplacement : common.util car c'est une fonction utilitaire système
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionService {

    private final SendEventRepository sendEventRepository;
    private final TrackingEventRepository trackingEventRepository;

    // ✅ Rétention : 90 jours pour les événements de tracking
    private static final int RETENTION_DAYS = 90;

    // ✅ Nettoyage quotidien à 3h du matin
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanOldData() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("🔄 Nettoyage des données antérieures à {}", cutoff);

        try {
            // 1. Supprimer les anciens TrackingEvent
            long trackingDeleted = trackingEventRepository.deleteByOccurredAtBefore(cutoff);
            log.info("✅ {} TrackingEvent supprimés", trackingDeleted);

            // 2. Supprimer les anciens SendEvent (sans événements associés)
            long sendDeleted = sendEventRepository.deleteOrphanSendEvents(cutoff);
            log.info("✅ {} SendEvent orphelins supprimés", sendDeleted);

            log.info("✅ Nettoyage terminé avec succès");

        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage des données : {}", e.getMessage(), e);
        }
    }

    // ✅ Méthode manuelle pour un nettoyage forcé (admin)
    @Transactional
    public long forceClean(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        log.warn("⚠️ Nettoyage forcé des données antérieures à {} jours", days);

        long trackingDeleted = trackingEventRepository.deleteByOccurredAtBefore(cutoff);
        long sendDeleted = sendEventRepository.deleteOrphanSendEvents(cutoff);

        log.info("✅ Nettoyage forcé : {} TrackingEvent, {} SendEvent supprimés",
                trackingDeleted, sendDeleted);

        return trackingDeleted + sendDeleted;
    }
}