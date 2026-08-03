package com.intellisec.phishsim.tracking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;

    // ── 1. TRACKING PIXEL — OPEN ──────────────────────
    @GetMapping(value = "/pixel/{token}", produces = MediaType.IMAGE_GIF_VALUE)
    public ResponseEntity<byte[]> trackPixel(
            @PathVariable String token,
            HttpServletRequest request) {

        String userAgent = request.getHeader("User-Agent");
        String ipHash = hashIp(request.getRemoteAddr());
        trackingService.trackOpen(token, userAgent, ipHash);

        // Pixel GIF 1x1 transparent
        byte[] pixel = Base64.getDecoder().decode(
                "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
        );
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_GIF)
                .body(pixel);
    }

    // ── 2. CLICK TRACKING & REDIRECT ──────────────────
    @GetMapping("/click/{token}")
    public void trackClick(
            @PathVariable String token,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String userAgent = request.getHeader("User-Agent");
        String ipHash = hashIp(request.getRemoteAddr());
        trackingService.trackClick(token, userAgent, ipHash);

        String redirectUrl = "http://localhost:8086/track/landing/" + token;
        log.info("🔀 Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    // ── 3. LANDING PAGE ───────────────────────────────
    @GetMapping(value = "/landing/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landingPage(@PathVariable String token) {
        log.info("🔍 Landing page requested with token: {}", token);
        try {
            SendEvent sendEvent = trackingService.getByToken(token);
            log.info("✅ Token found for landing: {}", token);

            String html = buildLandingPage(token);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (Exception e) {
            log.error("❌ Token not found or error: {}", token, e);
            return ResponseEntity.notFound().build();
        }
    }

    // ── 4. SUBMISSION CAPTURE ─────────────────────────
    @PostMapping("/submit/{token}")
    public ResponseEntity<String> trackSubmit(
            @PathVariable String token,
            @RequestBody(required = false) Object body,
            HttpServletRequest request) {

        String userAgent = request.getHeader("User-Agent");
        String ipHash = hashIp(request.getRemoteAddr());

        // ⚠️ GUARDRAIL : on ignore complètement le body (credentials)
        // On enregistre UNIQUEMENT le fait de la soumission
        trackingService.trackSubmit(token, userAgent, ipHash);

        return ResponseEntity.ok("submitted");
    }

    // ── 5. TIMELINE PAR CAMPAGNE ──────────────────────
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<SendEvent>> getCampaignEvents(
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(trackingService.getEventsByCampaign(campaignId));
    }

    // ── 6. TIMELINE PAR SEND EVENT ────────────────────
    @GetMapping("/events/{sendEventId}")
    public ResponseEntity<List<TrackingEvent>> getSendEventTimeline(
            @PathVariable UUID sendEventId) {
        return ResponseEntity.ok(trackingService.getEventsBySendEvent(sendEventId));
    }

    // ── Helper : Hash IP ──────────────────────────────
    private String hashIp(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ── Helper : Build Landing Page HTML ─────────────
    private String buildLandingPage(String token) {
        // Échapper les '%' littéraux en les doublant pour éviter les erreurs de format
        String html = """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Connexion — IntellisecSollutions</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: Arial, sans-serif; background: #f0f4f8; display: flex; align-items: center; justify-content: center; min-height: 100vh; }
                    .card { background: white; padding: 40px; border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); width: 100%%; max-width: 400px; }
                    .logo { text-align: center; margin-bottom: 32px; font-size: 24px; font-weight: 800; color: #1a3a5c; }
                    .logo span { color: #e07b2a; }
                    h2 { color: #1a3a5c; margin-bottom: 24px; font-size: 20px; }
                    label { display: block; color: #555; font-size: 13px; margin-bottom: 6px; font-weight: 600; }
                    input { width: 100%%; padding: 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; margin-bottom: 16px; }
                    button { width: 100%%; padding: 12px; background: #e07b2a; color: white; border: none; border-radius: 6px; font-size: 16px; font-weight: 700; cursor: pointer; }
                    .info { font-size: 12px; color: #888; text-align: center; margin-top: 16px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">INTELLI<span>SECSollutions</span></div>
                    <h2>Connexion requise</h2>
                    <form id="loginForm">
                        <label>Adresse email</label>
                        <input type="email" id="email" placeholder="votre@email.com" required>
                        <label>Mot de passe</label>
                        <input type="password" id="password" placeholder="••••••••" required>
                        <button type="submit">Se connecter</button>
                    </form>
                    <p class="info">Connexion sécurisée SSL</p>
                </div>
                <script>
                    document.getElementById('loginForm').addEventListener('submit', function(e) {
                        e.preventDefault();
                        // ⚠️ On envoie UNIQUEMENT la notification — pas les valeurs
                        fetch('/track/submit/%s', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({}) // body vide intentionnellement
                        }).then(() => {
                            // Redirection vers page de sensibilisation
                            window.location.href = '/track/awareness/%s';
                        });
                    });
                </script>
            </body>
            </html>
            """;
        // Remplacer les placeholders %s par le token
        return html.formatted(token, token);
    }

    // ── 7. PAGE DE SENSIBILISATION ────────────────────
    @GetMapping(value = "/awareness/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> awarenessPage(@PathVariable String token) {
        String html = """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <title>⚠️ Simulation de Phishing</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: Arial, sans-serif; background: #0a1628; display: flex; align-items: center; justify-content: center; min-height: 100vh; }
                .card { background: white; padding: 40px; border-radius: 12px; width: 100%; max-width: 600px; }
                .header { background: #e07b2a; color: white; padding: 20px; border-radius: 8px; margin-bottom: 24px; text-align: center; }
                .header h1 { font-size: 24px; margin-bottom: 8px; }
                h3 { color: #1a3a5c; margin: 16px 0 8px 0; }
                ul { padding-left: 20px; color: #444; line-height: 1.8; }
                .footer { background: #f0f4f8; padding: 16px; border-radius: 8px; margin-top: 24px; text-align: center; color: #555; font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="header">
                    <h1>⚠️ Vous venez d'être victime d'une simulation de phishing</h1>
                    <p>Ceci était un test de sécurité organisé par Intellisec Solutions</p>
                </div>

                <h3>🚨 Ce que vous auriez dû remarquer :</h3>
                <ul>
                    <li>L'expéditeur du mail ne correspond pas au domaine officiel</li>
                    <li>L'URL du lien ne pointe pas vers le vrai site</li>
                    <li>Le mail crée une urgence artificielle pour vous faire agir vite</li>
                    <li>Le formulaire de connexion est hébergé sur un domaine suspect</li>
                </ul>

                <h3>✅ Ce que vous devez faire face à un vrai mail suspect :</h3>
                <ul>
                    <li>Ne cliquez pas sur les liens — survolez-les d'abord pour voir l'URL réelle</li>
                    <li>Vérifiez l'adresse email de l'expéditeur</li>
                    <li>Contactez directement l'expéditeur présumé par un autre canal</li>
                    <li>Signalez le mail à votre équipe sécurité</li>
                </ul>

                <div class="footer">
                    <strong>Intellisec Solutions — Formation à la cybersécurité</strong><br>
                    Vos données n'ont pas été compromises. Aucun mot de passe n'a été enregistré.
                </div>
            </div>
        </body>
        </html>
        """;
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}