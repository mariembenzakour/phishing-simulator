package com.intellisec.phishsim.tracking;

import com.intellisec.phishsim.ai.AiGenerationLog;
import com.intellisec.phishsim.ai.AiGenerationLogRepository;
import com.intellisec.phishsim.campaign.Campaign;
import com.intellisec.phishsim.campaign.CampaignRepository;
import com.intellisec.phishsim.common.config.UrlConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;
    private final UrlConfig urlConfig;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final CampaignRepository campaignRepository;

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

        String landingUrl = urlConfig.getLandingPageUrl(token);
        log.info("🔄 Redirection vers: {}", landingUrl);
        response.sendRedirect(landingUrl);
    }

    // ── 3. LANDING PAGE ────────────────────────────
    @GetMapping(value = "/landing/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landingPage(@PathVariable String token) {
        log.info("🔍 Landing page requested with token: {}", token);

        try {
            SendEvent sendEvent = trackingService.getByToken(token);
            Campaign campaign = campaignRepository.findById(sendEvent.getCampaignId())
                    .orElseThrow(() -> new RuntimeException("Campaign not found"));

            String landingPageHtml = getLandingPageFromCampaign(campaign);

            if (landingPageHtml == null || landingPageHtml.isEmpty()) {
                log.warn("⚠️ Aucune landing page IA trouvée, utilisation du fallback");
                String fallbackHtml = buildDefaultLandingPage(token);
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(fallbackHtml);
            }

            String html = injectTokenInLandingPage(landingPageHtml, token);

            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);

        } catch (Exception e) {
            log.error("❌ Erreur landing page: {}", e.getMessage(), e);
            String errorHtml = buildErrorPage(token, e.getMessage());
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }

    // ── 4. SUBMISSION CAPTURE ──────────────────────
    @PostMapping("/submit/{token}")
    public ResponseEntity<String> trackSubmit(@PathVariable String token,
                                              HttpServletRequest request) {
        log.info("📝 SUBMIT request for token: {}", token);
        log.info("📝 Content-Type: {}", request.getContentType());

        try {
            String userAgent = request.getHeader("User-Agent");
            String ipHash = hashIp(request.getRemoteAddr());

            trackingService.trackSubmit(token, userAgent, ipHash);

            log.info("✅ SUBMIT tracked for token: {}", token);

            String awarenessUrl = urlConfig.getAwarenessPageUrl(token);
            log.info("🔄 Redirection vers: {}", awarenessUrl);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", awarenessUrl)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors du tracking de soumission: {}", e.getMessage(), e);
            String awarenessUrl = urlConfig.getAwarenessPageUrl(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", awarenessUrl)
                    .build();
        }
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
        log.info("🔍 Awareness page requested with token: {}", token);

        try {
            SendEvent sendEvent = trackingService.getByToken(token);
            Campaign campaign = campaignRepository.findById(sendEvent.getCampaignId())
                    .orElseThrow(() -> new RuntimeException("Campaign not found"));

            String awarenessPageHtml = getAwarenessPageFromCampaign(campaign);
            String redFlagsJson = getRedFlagsFromCampaign(campaign);

            if (awarenessPageHtml == null || awarenessPageHtml.isEmpty()) {
                log.warn("⚠️ Aucune awareness page IA trouvée, utilisation du fallback");
                String defaultPage = buildDefaultAwarenessPage(redFlagsJson);
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(defaultPage);
            }

            String html = injectRedFlagsInAwarenessPage(awarenessPageHtml, redFlagsJson);

            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);

        } catch (Exception e) {
            log.error("❌ Erreur lors du chargement de l'awareness page: {}", e.getMessage(), e);
            String errorPage = buildErrorPage(token, e.getMessage());
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorPage);
        }
    }

    // ============================================================
    // DASHBOARD
    // ============================================================

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

    // ============================================================
    // ✅ TIME-TO-CLICK ENDPOINTS
    // ============================================================

    @GetMapping("/campaign/{campaignId}/time-to-click")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getTimeToClick(@PathVariable UUID campaignId) {
        Map<String, Object> stats = trackingService.getTimeToClickStats(campaignId);
        stats.put("campaignId", campaignId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user/{targetId}/time-to-click")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, Object>> getUserTimeToClick(@PathVariable UUID targetId) {
        Map<String, Object> stats = trackingService.getUserTimeToClickStats(targetId);
        stats.put("targetId", targetId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/campaign/{campaignId}/users/time-to-click")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<List<Map<String, Object>>> getUsersTimeToClick(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(trackingService.getUsersTimeToClick(campaignId));
    }

    @GetMapping("/user/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<List<Map<String, Object>>> getUserStats() {
        return ResponseEntity.ok(trackingService.getUserStats());
    }

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

    private String getLandingPageFromCampaign(Campaign campaign) {
        if (campaign.getAiGenerationId() == null) {
            return null;
        }
        AiGenerationLog aiLog = aiGenerationLogRepository.findById(campaign.getAiGenerationId()).orElse(null);
        return aiLog != null ? aiLog.getLandingPageHtml() : null;
    }

    private String getAwarenessPageFromCampaign(Campaign campaign) {
        if (campaign.getAiGenerationId() == null) {
            return null;
        }
        AiGenerationLog aiLog = aiGenerationLogRepository.findById(campaign.getAiGenerationId()).orElse(null);
        return aiLog != null ? aiLog.getAwarenessPageHtml() : null;
    }

    private String getRedFlagsFromCampaign(Campaign campaign) {
        if (campaign.getAiGenerationId() == null) {
            return null;
        }
        AiGenerationLog aiLog = aiGenerationLogRepository.findById(campaign.getAiGenerationId()).orElse(null);
        return aiLog != null ? aiLog.getRedFlags() : null;
    }

    private String injectTokenInLandingPage(String html, String token) {
        String submitUrl = urlConfig.getSubmitUrl(token);
        String awarenessUrl = urlConfig.getAwarenessPageUrl(token);
        String trackingUrl = urlConfig.getTrackingClickUrl(token);
        String pixelUrl = urlConfig.getTrackingPixelUrl(token);

        html = html.replace("SUBMIT_URL", submitUrl);
        html = html.replace("AWARENESS_URL", awarenessUrl);
        html = html.replace("TRACKING_LINK", trackingUrl);
        html = html.replace("TRACKING_PIXEL", pixelUrl);

        html = html.replace("action=\"/track/submit/", "action=\"" + submitUrl + "\"");
        html = html.replace("action='/track/submit/", "action='" + submitUrl + "'");

        html = html.replace("href=\"/track/awareness/", "href=\"" + awarenessUrl + "\"");
        html = html.replace("href='/track/awareness/", "href='" + awarenessUrl + "'");

        html = html.replace("fetch('/track/submit/", "fetch('" + submitUrl + "'");
        html = html.replace("fetch(\"/track/submit/", "fetch(\"" + submitUrl + "\"");

        return html;
    }

    /**
     * ✅ Injecte les red flags dans l'awareness page
     */
    private String injectRedFlagsInAwarenessPage(String html, String redFlagsJson) {
        if (redFlagsJson == null || redFlagsJson.isEmpty()) {
            return html;
        }

        StringBuilder redFlagsHtml = new StringBuilder();
        redFlagsHtml.append("<div class=\"red-flags-container\">");
        redFlagsHtml.append("<h3>🚨 Signes d'une tentative de phishing :</h3>");
        redFlagsHtml.append("<ul>");

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> redFlags = mapper.readValue(redFlagsJson, List.class);

            for (Map<String, Object> flag : redFlags) {
                String severity = (String) flag.getOrDefault("severity", "medium");
                String title = (String) flag.getOrDefault("title", "Alerte");
                String description = (String) flag.getOrDefault("description", "");
                String howToDetect = (String) flag.getOrDefault("howToDetect", "");

                String severityColor = switch (severity) {
                    case "critical" -> "#dc2626";
                    case "high" -> "#f59e0b";
                    case "medium" -> "#f97316";
                    default -> "#22c55e";
                };

                String severityIcon = switch (severity) {
                    case "critical" -> "🔴";
                    case "high" -> "🟠";
                    case "medium" -> "🟡";
                    default -> "🟢";
                };

                redFlagsHtml.append("<li style=\"border-left: 4px solid ").append(severityColor).append("; padding: 8px 12px; margin-bottom: 8px; background: #f8fafc; border-radius: 4px;\">");
                redFlagsHtml.append("<strong>").append(severityIcon).append(" ").append(title).append("</strong>");
                if (!description.isEmpty()) {
                    redFlagsHtml.append("<p style=\"margin: 4px 0; color: #555; font-size: 14px;\">").append(description).append("</p>");
                }
                if (!howToDetect.isEmpty()) {
                    redFlagsHtml.append("<p style=\"margin: 4px 0; color: #22c55e; font-size: 13px;\">✅ ").append(howToDetect).append("</p>");
                }
                redFlagsHtml.append("</li>");
            }
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors du parsing des red flags: {}", e.getMessage());
            redFlagsHtml.append("<li>⚠️ Les red flags ne sont pas disponibles.</li>");
        }

        redFlagsHtml.append("</ul>");
        redFlagsHtml.append("</div>");

        html = html.replace("RED_FLAGS_CONTENT", redFlagsHtml.toString());

        return html;
    }

    /**
     * ⚠️ FALLBACK : Landing page par défaut
     */
    private String buildDefaultLandingPage(String token) {
        String submitUrl = urlConfig.getSubmitUrl(token);
        String awarenessUrl = urlConfig.getAwarenessPageUrl(token);

        return """
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Connexion — Intellisec Solutions</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f4f8; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; }
            .card { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); width: 100%%; max-width: 400px; text-align: center; }
            .logo { font-size: 28px; font-weight: 800; color: #1a3a5c; margin-bottom: 4px; }
            .logo span { color: #e07b2a; }
            .subtitle { font-size: 12px; color: #8899aa; letter-spacing: 3px; margin-bottom: 24px; }
            h2 { color: #1a3a5c; margin-bottom: 24px; font-size: 20px; }
            label { display: block; color: #555; font-size: 13px; margin-bottom: 6px; font-weight: 600; text-align: left; }
            input { width: 100%%; padding: 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; margin-bottom: 16px; }
            button { width: 100%%; padding: 12px; background: #e07b2a; color: white; border: none; border-radius: 6px; font-size: 16px; font-weight: 700; cursor: pointer; }
            button:hover { background: #c96a20; }
            .info { font-size: 12px; color: #888; margin-top: 16px; }
        </style>
    </head>
    <body>
        <div class="card">
            <div class="logo">INTELLI<span>SEC</span></div>
            <div class="subtitle">SOLUTIONS</div>
            <h2>🔐 Connexion requise</h2>
            <form id="loginForm" action="%s" method="POST">
                <label>Adresse email</label>
                <input type="email" name="email" placeholder="votre@email.com" required>
                <label>Mot de passe</label>
                <input type="password" name="password" placeholder="••••••••" required>
                <button type="submit">Se connecter</button>
            </form>
            <p class="info">⚠️ Ceci est une simulation de phishing</p>
        </div>
        <script>
            document.getElementById('loginForm').addEventListener('submit', function(e) {
                e.preventDefault();
                fetch(this.action, {
                    method: 'POST',
                    body: new FormData(this)
                }).then(() => {
                    window.location.href = '%s';
                });
            });
        </script>
    </body>
    </html>
    """.formatted(submitUrl, awarenessUrl);
    }

    /**
     * ⚠️ FALLBACK : Awareness page par défaut avec red flags
     */
    private String buildDefaultAwarenessPage(String redFlagsJson) {
        // ✅ Construire les red flags par défaut
        String redFlagsHtml = """
        <div class="red-flags-container">
            <h3>🚨 Signes d'une tentative de phishing :</h3>
            <ul>
                <li style="border-left: 4px solid #dc2626; padding: 8px 12px; margin-bottom: 8px; background: #f8fafc; border-radius: 4px;">
                    <strong>🔴 Expéditeur suspect</strong>
                    <p style="margin: 4px 0; color: #555; font-size: 14px;">L'email ne provient pas d'un domaine officiel.</p>
                    <p style="margin: 4px 0; color: #22c55e; font-size: 13px;">✅ Vérifiez toujours l'adresse email de l'expéditeur.</p>
                </li>
                <li style="border-left: 4px solid #f59e0b; padding: 8px 12px; margin-bottom: 8px; background: #f8fafc; border-radius: 4px;">
                    <strong>🟠 Urgence artificielle</strong>
                    <p style="margin: 4px 0; color: #555; font-size: 14px;">Le message vous pousse à agir rapidement sans réfléchir.</p>
                    <p style="margin: 4px 0; color: #22c55e; font-size: 13px;">✅ Prenez le temps de vérifier avant d'agir.</p>
                </li>
                <li style="border-left: 4px solid #f59e0b; padding: 8px 12px; margin-bottom: 8px; background: #f8fafc; border-radius: 4px;">
                    <strong>🟠 Lien suspect</strong>
                    <p style="margin: 4px 0; color: #555; font-size: 14px;">Le lien ne correspond pas au site officiel.</p>
                    <p style="margin: 4px 0; color: #22c55e; font-size: 13px;">✅ Survolez le lien avant de cliquer pour vérifier l'URL.</p>
                </li>
                <li style="border-left: 4px solid #dc2626; padding: 8px 12px; margin-bottom: 8px; background: #f8fafc; border-radius: 4px;">
                    <strong>🔴 Demande d'informations personnelles</strong>
                    <p style="margin: 4px 0; color: #555; font-size: 14px;">L'email vous demande votre mot de passe ou des informations confidentielles.</p>
                    <p style="margin: 4px 0; color: #22c55e; font-size: 13px;">✅ Ne partagez jamais vos identifiants par email.</p>
                </li>
            </ul>
        </div>
        """;

        // ✅ Si on a des red flags de l'IA, on les utilise
        if (redFlagsJson != null && !redFlagsJson.isEmpty()) {
            String parsedFlags = injectRedFlagsInAwarenessPage("RED_FLAGS_CONTENT", redFlagsJson);
            if (!parsedFlags.contains("RED_FLAGS_CONTENT")) {
                redFlagsHtml = parsedFlags;
            }
        }

        // ✅ Retourner le HTML SANS String.formatted() pour éviter les problèmes de %
        return """
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>⚠️ Simulation de Phishing - Intellisec</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', Arial, sans-serif; 
            background: #0a1628; 
            display: flex; 
            align-items: center; 
            justify-content: center; 
            min-height: 100vh;
            padding: 20px;
        }
        .card { 
            background: white; 
            padding: 40px; 
            border-radius: 16px; 
            max-width: 700px; 
            width: 100%;
            box-shadow: 0 20px 60px rgba(0,0,0,0.5);
        }
        .header { 
            background: #e07b2a; 
            color: white; 
            padding: 20px; 
            border-radius: 12px; 
            margin-bottom: 24px; 
            text-align: center; 
        }
        .header h1 { 
            font-size: 24px; 
            margin-bottom: 8px; 
        }
        .header p {
            font-size: 14px;
            opacity: 0.9;
        }
        .footer { 
            background: #f0f4f8; 
            padding: 16px; 
            border-radius: 8px; 
            margin-top: 24px; 
            text-align: center; 
            color: #555; 
            font-size: 13px; 
        }
        .logo { 
            font-size: 24px; 
            font-weight: 800; 
            color: #1a3a5c; 
            text-align: center;
            margin-bottom: 8px;
        }
        .logo span { color: #e07b2a; }
        .red-flags-container {
            margin: 16px 0;
        }
        .red-flags-container h3 {
            color: #1a3a5c;
            margin-bottom: 12px;
        }
        .red-flags-container ul {
            list-style: none;
            padding: 0;
        }
        .red-flags-container ul li {
            border-left: 4px solid #dc2626;
            padding: 8px 12px;
            margin-bottom: 8px;
            background: #f8fafc;
            border-radius: 4px;
        }
        .red-flags-container ul li strong {
            display: block;
            margin-bottom: 4px;
            color: #1a3a5c;
        }
        .red-flags-container ul li p {
            margin: 4px 0;
            color: #555;
            font-size: 14px;
        }
        .red-flags-container ul li .detect {
            color: #22c55e;
            font-size: 13px;
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="logo">INTELLI<span>SEC</span></div>
        <div class="header">
            <h1>⚠️ Simulation de phishing</h1>
            <p>Test de sécurité organisé par Intellisec Solutions</p>
        </div>
        %s
        <div class="footer">
            <strong>Intellisec Solutions — Formation cybersécurité</strong><br>
            Vos données n'ont pas été compromises.
        </div>
    </div>
</body>
</html>
""".formatted(redFlagsHtml);
    }

    /**
     * ⚠️ Page d'erreur en cas de problème
     */
    private String buildErrorPage(String token, String error) {
        return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>Erreur</title>
        <style>
            body { font-family: 'Segoe UI', Arial, sans-serif; padding: 40px; background: #f5f5f5; display: flex; align-items: center; justify-content: center; min-height: 100vh; }
            .card { max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
            h1 { color: #dc2626; margin-top: 0; }
            .error-detail { color: #666; font-size: 14px; background: #f8f8f8; padding: 12px; border-radius: 6px; margin: 16px 0; word-break: break-word; }
            a { color: #e07b2a; text-decoration: none; }
            a:hover { text-decoration: underline; }
        </style>
    </head>
    <body>
        <div class="card">
            <h1>❌ Une erreur est survenue</h1>
            <p>Nous n'avons pas pu charger la page demandée.</p>
            <div class="error-detail">%s</div>
            <p>
                <a href="/">← Retour à l'accueil</a>
            </p>
        </div>
    </body>
    </html>
    """.formatted(error != null ? error : "Erreur inconnue");
    }
}