package com.intellisec.phishsim.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    @Value("${ai.gemini.api.key}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-3.6-flash}")
    private String model;

    @Value("${ai.generation.max-tokens:8192}")
    private int maxTokens;

    @Value("${ai.generation.temperature:0.7}")
    private double temperature;

    @Value("${ai.default-language:fr}")
    private String defaultLanguage;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiGenerationLogRepository logRepository;

    public GenerationResponse generateEmail(GenerationRequest request) {
        try {
            String prompt = buildFullPrompt(request);
            log.info("📤 PROMPT envoyé à Gemini:\n{}", prompt);

            String geminiResponse = callGeminiApi(prompt);
            log.info("📥 RÉPONSE BRUTE DE GEMINI:\n{}", geminiResponse);

            GenerationResponse response = parseGeminiResponse(geminiResponse);

            response.setLanguage(request.getLanguage() != null ? request.getLanguage() : defaultLanguage);
            response.setScenario(request.getScenario());
            response.setStatus("DRAFT");
            response.setApproved(false);
            response.setGeneratedAt(LocalDateTime.now().toString());

            saveGenerationLog(request, response, prompt);

            log.info("✅ Email généré pour: {}", request.getScenario());

            return response;

        } catch (Exception e) {
            log.error("❌ Erreur de génération IA: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur de génération", e);
        }
    }

    private String buildFullPrompt(GenerationRequest request) {
        String language = request.getLanguage() != null ? request.getLanguage() : defaultLanguage;

        String languageInstructions = "fr".equals(language)
                ? "IMPORTANT: The ENTIRE content MUST be written in French (Quebec French)."
                : "IMPORTANT: The ENTIRE content MUST be written in English.";

        return """
You are a cybersecurity awareness training generator creating educational templates for authorized employee simulations.

=== SAFETY & COMPLIANCE DIRECTIVES ===
- This content is strictly for internal security awareness training.
- Generate standard corporate communication templates (e.g., IT service announcements, system verification requests).
- Do NOT generate malicious payloads, exploit code, or actual malware.

=== LANGUAGE ===
%s

=== SCENARIO PARAMETERS ===
Scenario: %s
Target Department: %s
Urgency Level: %s
Additional Context: %s

=== CONTENT REQUIREMENTS ===

1. SIMULATED EMAIL TEMPLATE (`subject`, `bodyHtml`, `bodyText`, `senderName`, `senderDomain`):
   - A professional corporate email template matching the scenario (e.g., IT security notice or system update).
   - HTML layout: max-width 600px, responsive, professional inline CSS.
   - Use placeholders: {{firstName}}, {{lastName}}, and TRACKING_LINK for the call-to-action link.

2. SIMULATED LANDING PAGE TEMPLATE (`landingPageHtml`):
   - A simulated portal interface corresponding to the scenario (e.g., corporate verification portal or access request page).
   - Form attributes: action="SUBMIT_URL" method="POST".
   - Clean, professional design suitable for a simulated training exercise.

3. AWARENESS DEBRIEF PAGE (`awarenessPageHtml`):
   - The post-click educational page informing the employee that this was an authorized simulation by the society.
   - Must include the placeholder RED_FLAGS_CONTENT.

4. EDUCATIONAL RED FLAGS (`redFlags`):
   - Array of 4 educational red flags present in the email/landing page.
   - JSON structure per item: type, title, description, severity, howToDetect.

=== OUTPUT FORMAT ===
Respond ONLY with a valid JSON object matching this schema:
{
    "subject": "...",
    "bodyHtml": "...",
    "bodyText": "...",
    "senderName": "...",
    "senderDomain": "...",
    "landingPageHtml": "...",
    "awarenessPageHtml": "...",
    "redFlags": [
        {"type": "sender_domain", "title": "...", "description": "...", "severity": "critical", "howToDetect": "..."},
        {"type": "urgency_pressure", "title": "...", "description": "...", "severity": "high", "howToDetect": "..."},
        {"type": "link_mismatch", "title": "...", "description": "...", "severity": "critical", "howToDetect": "..."},
        {"type": "unusual_request", "title": "...", "description": "...", "severity": "high", "howToDetect": "..."}
    ]
}
""".formatted(
                languageInstructions,
                request.getScenario() != null ? request.getScenario() : "Internal IT Update",
                request.getDepartment() != null ? request.getDepartment() : "All employees",
                request.getUrgency() != null ? request.getUrgency() : "medium",
                request.getAdditionalDetails() != null ? request.getAdditionalDetails() : "Standard corporate communication"
        );
    }
    private void saveGenerationLog(GenerationRequest request, GenerationResponse response, String prompt) {
        try {
            AiGenerationLog generationLog = new AiGenerationLog();
            generationLog.setScenario(request.getScenario());
            generationLog.setLanguage(request.getLanguage() != null ? request.getLanguage() : defaultLanguage);
            generationLog.setPrompt(prompt);
            generationLog.setGeneratedSubject(response.getSubject());
            generationLog.setGeneratedBody(response.getBodyHtml());
            generationLog.setBodyText(response.getBodyText());
            generationLog.setLandingPageHtml(response.getLandingPageHtml());
            // ✅ NOUVEAU : Sauvegarder l'awareness page
            generationLog.setAwarenessPageHtml(response.getAwarenessPageHtml());
            generationLog.setRedFlags(response.getRedFlags());
            generationLog.setSenderName(response.getSenderName());
            generationLog.setSenderDomain(response.getSenderDomain());
            generationLog.setGeneratedBy(getCurrentUser());
            generationLog.setApproved(false);

            AiGenerationLog saved = logRepository.save(generationLog);
            response.setId(saved.getId().toString());

            log.info("💾 Log de génération sauvegardé avec ID: {}", saved.getId());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la sauvegarde du log: {}", e.getMessage());
        }
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null ? auth.getName() : "SYSTEM";
    }

    private String callGeminiApi(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        ));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("topP", 0.95);
        // Demande explicitement à Gemini de répondre en JSON pur (supporté par l'API Gemini)
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 3;
        int retryDelay = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("📤 Appel Gemini - tentative {}/{}", attempt, maxRetries);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    return response.getBody();
                }

                if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    log.warn("⚠️ Gemini surchargé (503), tentative {}/{}", attempt, maxRetries);
                    if (attempt < maxRetries) {
                        Thread.sleep(retryDelay);
                        continue;
                    }
                }

                throw new RuntimeException("Erreur API Gemini: " + response.getStatusCode());

            } catch (HttpServerErrorException.ServiceUnavailable e) {
                log.warn("⚠️ Gemini surchargé, tentative {}/{}", attempt, maxRetries);
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                throw new RuntimeException("Gemini surchargé après " + maxRetries + " tentatives", e);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interruption pendant le retry", e);
            } catch (Exception e) {
                log.error("❌ Exception appel Gemini: {}", e.getMessage(), e);
                throw new RuntimeException("Erreur appel Gemini: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("Erreur appel Gemini après " + maxRetries + " tentatives");
    }

    private GenerationResponse parseGeminiResponse(String responseBody) throws Exception {
        log.info("📥 Parsing de la réponse: {}", responseBody);

        JsonNode root = objectMapper.readTree(responseBody);

        if (root.has("error")) {
            String errorMsg = root.path("error").path("message").asText();
            log.error("❌ Erreur Gemini: {}", errorMsg);
            throw new RuntimeException("Erreur Gemini: " + errorMsg);
        }

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            log.error("❌ Aucun candidat retourné par Gemini. Réponse complète: {}", responseBody);
            throw new RuntimeException("Aucun candidat retourné par Gemini (contenu potentiellement bloqué par les filtres de sécurité)");
        }

        String finishReason = root.path("candidates").path(0).path("finishReason").asText("STOP");
        if ("MAX_TOKENS".equals(finishReason)) {
            log.error("❌ Réponse tronquée (MAX_TOKENS) — le JSON est probablement incomplet. " +
                    "Augmentez ai.generation.max-tokens (valeur actuelle: {}).", maxTokens);
            throw new RuntimeException(
                    "La réponse de Gemini a été tronquée (MAX_TOKENS). Augmentez ai.generation.max-tokens.");
        }
        if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
            log.error("❌ Réponse bloquée par Gemini, finishReason={}", finishReason);
            throw new RuntimeException("Réponse bloquée par Gemini (finishReason=" + finishReason + ")");
        }

        JsonNode partsNode = root.path("candidates").path(0).path("content").path("parts");
        if (!partsNode.isArray() || partsNode.isEmpty()) {
            log.error("❌ Aucune partie de contenu dans la réponse Gemini: {}", responseBody);
            throw new RuntimeException("Réponse Gemini vide (pas de 'parts' dans le contenu)");
        }

        String text = partsNode.path(0).path("text").asText();

        log.info("📝 Texte généré ({} caractères): {}", text.length(), text);

        if (text == null || text.isBlank()) {
            throw new RuntimeException("Le texte généré par Gemini est vide");
        }

        String jsonText = extractJsonFromResponse(text);

        log.info("📝 JSON extrait ({} caractères): {}", jsonText.length(), jsonText);

        if (jsonText.isEmpty() || jsonText.equals("{}")) {
            throw new RuntimeException("Le texte généré ne contient pas de JSON valide");
        }

        try {
            return mapJsonToResponse(jsonText);
        } catch (Exception parseEx) {
            log.error("❌ Échec du parsing JSON. Texte extrait:\n{}", jsonText);
            throw new RuntimeException("JSON extrait invalide: " + parseEx.getMessage(), parseEx);
        }
    }

    /**
     * Construit un GenerationResponse à partir du JSON généré par Gemini.
     * On ne fait PAS de objectMapper.readValue(json, GenerationResponse.class) directement,
     * car "redFlags" est un TABLEAU JSON dans la réponse de Gemini (conformément au prompt),
     * alors que GenerationResponse.redFlags est un String (stocké tel quel, en TEXT, côté BDD).
     * On mappe donc champ par champ et on re-sérialise le tableau redFlags en chaîne JSON.
     */
    private GenerationResponse mapJsonToResponse(String jsonText) throws Exception {
        JsonNode node = objectMapper.readTree(jsonText);

        GenerationResponse response = new GenerationResponse();
        response.setSubject(node.path("subject").asText(null));
        response.setBodyHtml(node.path("bodyHtml").asText(null));
        response.setBodyText(node.path("bodyText").asText(null));
        response.setSenderName(node.path("senderName").asText(null));
        response.setSenderDomain(node.path("senderDomain").asText(null));
        response.setLandingPageHtml(node.path("landingPageHtml").asText(null));

        // ✅ NOUVEAU : Lire l'awareness page
        response.setAwarenessPageHtml(node.path("awarenessPageHtml").asText(null));

        JsonNode redFlagsNode = node.path("redFlags");
        if (redFlagsNode.isMissingNode() || redFlagsNode.isNull()) {
            log.warn("⚠️ Pas de 'redFlags' dans la réponse générée");
            response.setRedFlags("[]");
        } else if (redFlagsNode.isTextual()) {
            response.setRedFlags(redFlagsNode.asText());
        } else {
            response.setRedFlags(redFlagsNode.toString());
        }

        return response;
    }
    private String extractJsonFromResponse(String text) {
        // Retire les éventuelles balises de code markdown
        String cleaned = text
                .replaceAll("(?i)```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        int start = cleaned.indexOf('{');
        if (start < 0) {
            log.error("❌ Aucune accolade ouvrante trouvée dans la réponse");
            throw new RuntimeException("Aucun objet JSON trouvé dans la réponse de Gemini");
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return cleaned.substring(start, i + 1);
                }
            }
        }

        // On a atteint la fin du texte sans que les accolades ne se referment :
        // la réponse est très probablement tronquée.
        log.error("❌ Accolades non équilibrées — JSON probablement tronqué. Texte:\n{}", cleaned);
        throw new RuntimeException(
                "Impossible d'extraire un JSON valide : les accolades ne sont pas équilibrées " +
                        "(réponse probablement tronquée par Gemini)");
    }
}