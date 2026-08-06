package com.intellisec.phishsim.tracking;

import com.intellisec.phishsim.campaign.Campaign;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;

    // ── 1. TRACKING PIXEL ──────────────────────────
    @GetMapping(value = "/pixel/{token}", produces = MediaType.IMAGE_GIF_VALUE)
    public ResponseEntity<byte[]> trackPixel(@PathVariable String token, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipHash = hashIp(request.getRemoteAddr());
        trackingService.trackOpen(token, userAgent, ipHash);

        byte[] pixel = Base64.getDecoder().decode(
                "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
        );
        return ResponseEntity.ok().contentType(MediaType.IMAGE_GIF).body(pixel);
    }

    // ── 2. CLICK TRACKING ──────────────────────────
    @GetMapping("/click/{token}")
    public void trackClick(@PathVariable String token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userAgent = request.getHeader("User-Agent");
        String ipHash = hashIp(request.getRemoteAddr());
        trackingService.trackClick(token, userAgent, ipHash);
        response.sendRedirect("http://localhost:8086/track/landing/" + token);
    }

    // ── 3. LANDING PAGE ────────────────────────────
    @GetMapping(value = "/landing/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landingPage(@PathVariable String token) {
        log.info("🔍 Landing page requested with token: {}", token);
        try {
            trackingService.getByToken(token);
            String html = buildLandingPage(token);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            log.error("❌ Token not found: {}", token, e);
            return ResponseEntity.notFound().build();
        }
    }

    // ── 4. SUBMISSION CAPTURE ──────────────────────
    @PostMapping("/submit/{token}")
    public ResponseEntity<String> trackSubmit(@PathVariable String token, @RequestBody(required = false) Object body, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipHash = hashIp(request.getRemoteAddr());
        trackingService.trackSubmit(token, userAgent, ipHash);
        return ResponseEntity.ok("submitted");
    }

    // ── 5. TIMELINE PAR CAMPAGNE ──────────────────
    @GetMapping("/campaign/{campaignId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<SendEvent>> getCampaignEvents(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(trackingService.getEventsByCampaign(campaignId));
    }

    // ── 6. TIMELINE PAR SEND EVENT ────────────────
    @GetMapping("/events/{sendEventId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<TrackingEvent>> getSendEventTimeline(@PathVariable UUID sendEventId) {
        return ResponseEntity.ok(trackingService.getEventsBySendEvent(sendEventId));
    }

    // ── 7. AWARENESS PAGE ──────────────────────────
    @GetMapping(value = "/awareness/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> awarenessPage(@PathVariable String token) {
        String html = buildAwarenessPage();
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    // ============================================================
    // ✅ NOUVEAUX : POUR LE DASHBOARD
    // ============================================================

    // ── 8. GLOBAL DASHBOARD ─────────────────────────
    @GetMapping("/dashboard/global")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> getGlobalDashboard() {
        List<Campaign> campaigns = trackingService.getAllCampaigns();
        List<SendEvent> allEvents = trackingService.getAllSendEvents();

        long totalSent = allEvents.size();
        long totalOpens = trackingService.countEventsByType("OPEN");
        long totalClicks = trackingService.countEventsByType("CLICK");
        long totalSubmits = trackingService.countEventsByType("SUBMIT");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalCampaigns", (long) campaigns.size());
        response.put("totalTargets", trackingService.getTotalTargets());
        response.put("totalSent", totalSent);
        response.put("totalReceived", totalSent);
        response.put("totalOpens", totalOpens);
        response.put("totalClicks", totalClicks);
        response.put("totalSubmits", totalSubmits);
        response.put("globalOpenRate", totalSent > 0 ? (totalOpens * 100.0 / totalSent) : 0);
        response.put("globalClickRate", totalSent > 0 ? (totalClicks * 100.0 / totalSent) : 0);
        response.put("globalSubmitRate", totalSent > 0 ? (totalSubmits * 100.0 / totalSent) : 0);

        List<Map<String, Object>> campaignList = new ArrayList<>();
        for (Campaign campaign : campaigns) {
            Map<String, Object> campaignData = new LinkedHashMap<>();
            campaignData.put("id", campaign.getId());
            campaignData.put("name", campaign.getName());
            campaignData.put("status", campaign.getStatus());
            campaignData.put("createdAt", campaign.getCreatedAt());

            Map<String, Object> stats = trackingService.getCampaignStats(campaign.getId());
            campaignData.put("sent", stats.get("sent"));
            campaignData.put("opens", stats.get("opens"));
            campaignData.put("clicks", stats.get("clicks"));
            campaignData.put("submits", stats.get("submits"));
            campaignData.put("openRate", stats.get("openRate"));
            campaignData.put("clickRate", stats.get("clickRate"));
            campaignData.put("submitRate", stats.get("submitRate"));

            campaignList.add(campaignData);
        }
        response.put("campaigns", campaignList);

        return ResponseEntity.ok(response);
    }

    // ── 9. CAMPAGNE DETAIL ──────────────────────────
    @GetMapping("/campaign/{campaignId}/details")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getCampaignDetails(@PathVariable UUID campaignId) {
        Campaign campaign = trackingService.getCampaignById(campaignId);
        Map<String, Object> stats = trackingService.getCampaignStats(campaignId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("campaignId", campaignId);
        response.put("campaignName", campaign.getName());
        response.put("status", campaign.getStatus());
        response.put("createdAt", campaign.getCreatedAt());
        response.put("stats", stats);

        return ResponseEntity.ok(response);
    }

    // ── 10. USER STATS ──────────────────────────────
    @GetMapping("/user/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<List<Map<String, Object>>> getUserStats() {
        return ResponseEntity.ok(trackingService.getUserStats());
    }

    // ── 11. USER HISTORY ────────────────────────────
    @GetMapping("/user/{targetId}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<List<TrackingEvent>> getUserHistory(@PathVariable UUID targetId) {
        return ResponseEntity.ok(trackingService.getUserHistory(targetId));
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private String hashIp(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ✅ CORRIGÉ : Utilisation de {token} au lieu de %%s
    private String buildLandingPage(String token) {
        String html = """
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <title>Connexion — Intellisec Solutions</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f4f8; display: flex; align-items: center; justify-content: center; min-height: 100vh; }
            .card { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); width: 100%%; max-width: 400px; text-align: center; }
            .logo { font-size: 28px; font-weight: 800; color: #1a3a5c; margin-bottom: 4px; }
            .logo span { color: #e07b2a; }
            .subtitle { font-size: 12px; color: #8899aa; letter-spacing: 3px; margin-bottom: 24px; }
            h2 { color: #1a3a5c; margin-bottom: 24px; font-size: 20px; }
            label { display: block; color: #555; font-size: 13px; margin-bottom: 6px; font-weight: 600; text-align: left; }
            input { width: 100%%; padding: 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; margin-bottom: 16px; }
            button { width: 100%%; padding: 12px; background: #e07b2a; color: white; border: none; border-radius: 6px; font-size: 16px; font-weight: 700; cursor: pointer; }
            .info { font-size: 12px; color: #888; margin-top: 16px; }
        </style>
    </head>
    <body>
        <div class="card">
            <div class="logo">INTELLI<span>SEC</span></div>
            <div class="subtitle">SOLUTIONS</div>
            <h2>🔐 Connexion requise</h2>
            <form id="loginForm">
                <label>Adresse email</label>
                <input type="email" id="email" placeholder="votre@email.com" required>
                <label>Mot de passe</label>
                <input type="password" id="password" placeholder="••••••••" required>
                <button type="submit">Se connecter</button>
            </form>
            <p class="info">Connexion sécurisée SSL — Intellisec Solutions</p>
        </div>
        <script>
            document.getElementById('loginForm').addEventListener('submit', function(e) {
                e.preventDefault();
                fetch('/track/submit/{token}', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({})
                }).then(() => {
                    window.location.href = '/track/awareness/{token}';
                });
            });
        </script>
    </body>
    </html>
    """;
        // ✅ Remplacer {token} par le vrai token
        return html.replace("{token}", token);
    }

    // ✅ CORRIGÉ : Retrait des %% dans le CSS
    private String buildAwarenessPage() {
        return """
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <title>⚠️ Simulation de Phishing</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { font-family: 'Segoe UI', Arial, sans-serif; background: #0a1628; display: flex; align-items: center; justify-content: center; min-height: 100vh; }
            .card { background: white; padding: 40px; border-radius: 12px; max-width: 600px; width: 90%; }
            .header { background: #e07b2a; color: white; padding: 20px; border-radius: 8px; margin-bottom: 24px; text-align: center; }
            .header h1 { font-size: 24px; margin-bottom: 8px; }
            h3 { color: #1a3a5c; margin: 16px 0 8px; }
            ul { padding-left: 20px; color: #444; line-height: 1.8; }
            .footer { background: #f0f4f8; padding: 16px; border-radius: 8px; margin-top: 24px; text-align: center; color: #555; font-size: 14px; }
        </style>
    </head>
    <body>
        <div class="card">
            <div class="header">
                <h1>⚠️ Simulation de phishing</h1>
                <p>Test de sécurité organisé par Intellisec Solutions</p>
            </div>
            <h3>🚨 Ce que vous auriez dû remarquer :</h3>
            <ul>
                <li>L'expéditeur ne correspond pas au domaine officiel</li>
                <li>L'URL ne pointe pas vers le vrai site</li>
                <li>Le mail crée une urgence artificielle</li>
            </ul>
            <h3>✅ Ce que vous devez faire :</h3>
            <ul>
                <li>Ne cliquez pas sur les liens suspects</li>
                <li>Vérifiez l'adresse email de l'expéditeur</li>
                <li>Signalez le mail à votre équipe sécurité</li>
            </ul>
            <div class="footer">
                <strong>Intellisec Solutions — Formation cybersécurité</strong><br>
                Vos données n'ont pas été compromises.
            </div>
        </div>
    </body>
    </html>
    """;
    }
}