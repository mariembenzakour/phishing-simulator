package com.intellisec.phishsim.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.*;

@Service
@Slf4j
public class DeliverabilityService {

    @Value("${app.email.sender-domain-check:true}")
    private boolean domainCheckEnabled;

    @Value("${app.rspamd.url:http://localhost:11333/checkv2}")
    private String rspamdUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeliverabilityService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ────────────────────────────────────────────────────────────────
    // 1. VÉRIFICATION DNS (SPF, DKIM, DMARC)
    // ────────────────────────────────────────────────────────────────

    /**
     * Vérifie les enregistrements DNS pour un domaine donné
     */
    public DomainCheckResult checkDomain(String domain) {
        log.info("🔍 Vérification DNS pour le domaine: {}", domain);

        DomainCheckResult result = new DomainCheckResult();
        result.setDomain(domain);
        result.setChecks(new HashMap<>());

        // Si la vérification DNS est désactivée dans la config (ex: app.email.sender-domain-check=false)
        if (!domainCheckEnabled) {
            log.warn("⚠️ Vérification DNS désactivée (Mode DEV).");
            result.setPass(true);
            result.setScore(10);
            result.setError("Mode dev: Vérification DNS ignorée");
            return result;
        }

        try {
            Map<String, Object> spfResult = checkSpf(domain);
            result.getChecks().put("spf", spfResult);

            Map<String, Object> dmarcResult = checkDmarc(domain);
            result.getChecks().put("dmarc", dmarcResult);

            Map<String, Object> dkimResult = checkDkim(domain, "default");
            result.getChecks().put("dkim", dkimResult);

            boolean spfPass = Boolean.TRUE.equals(spfResult.get("pass"));
            boolean dmarcPass = Boolean.TRUE.equals(dmarcResult.get("pass"));
            boolean dkimPass = Boolean.TRUE.equals(dkimResult.get("pass"));

            boolean allPass = spfPass && dmarcPass && dkimPass;

            result.setPass(allPass);
            result.setScore(allPass ? 10 :
                    (spfPass ? 4 : 0) +
                            (dmarcPass ? 3 : 0) +
                            (dkimPass ? 3 : 0));

            log.info("✅ DNS Check pour {}: pass={}, score={}", domain, allPass, result.getScore());

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification DNS pour {}: {}", domain, e.getMessage());
            // En cas d'erreur DNS (ex: domaine local non résolu), on ne fait pas planter l'application
            result.setPass(false);
            result.setScore(0);
            result.setError("Impossible de résoudre le domaine DNS public : " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> checkSpf(String domain) throws NamingException {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "SPF");

        List<String> records = queryTxt(domain);
        String spfRecord = records.stream()
                .filter(r -> r.startsWith("v=spf1"))
                .findFirst()
                .orElse(null);

        if (spfRecord != null) {
            result.put("pass", true);
            result.put("record", spfRecord);
            result.put("details", "Enregistrement SPF trouvé et valide");
        } else {
            result.put("pass", false);
            result.put("record", null);
            result.put("details", "Aucun enregistrement SPF trouvé pour ce domaine");
        }

        return result;
    }

    private Map<String, Object> checkDmarc(String domain) throws NamingException {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "DMARC");

        String dmarcDomain = "_dmarc." + domain;
        List<String> records = queryTxt(dmarcDomain);
        String dmarcRecord = records.stream()
                .filter(r -> r.startsWith("v=DMARC1"))
                .findFirst()
                .orElse(null);

        if (dmarcRecord != null) {
            result.put("pass", true);
            result.put("record", dmarcRecord);
            result.put("details", "Enregistrement DMARC trouvé et valide");
        } else {
            result.put("pass", false);
            result.put("record", null);
            result.put("details", "Aucun enregistrement DMARC trouvé pour ce domaine");
        }

        return result;
    }

    private Map<String, Object> checkDkim(String domain, String selector) throws NamingException {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "DKIM");

        String dkimDomain = selector + "._domainkey." + domain;
        List<String> records = queryTxt(dkimDomain);
        String dkimRecord = records.stream()
                .filter(r -> r.contains("v=DKIM1"))
                .findFirst()
                .orElse(null);

        if (dkimRecord != null) {
            result.put("pass", true);
            result.put("record", dkimRecord);
            result.put("details", "Enregistrement DKIM trouvé pour le sélecteur: " + selector);
        } else {
            result.put("pass", false);
            result.put("record", null);
            result.put("details", "Aucun enregistrement DKIM trouvé pour le sélecteur: " + selector);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> queryTxt(String domain) throws NamingException {
        List<String> results = new ArrayList<>();

        Properties env = new Properties();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns://8.8.8.8");

        DirContext ctx = new InitialDirContext(env);
        try {
            Attributes attrs = ctx.getAttributes(domain, new String[]{"TXT"});
            Attribute attr = attrs.get("TXT");

            if (attr != null) {
                for (Object val : Collections.list(attr.getAll())) {
                    String record = val.toString();
                    record = record.replaceAll("^\"|\"$", "");
                    results.add(record);
                }
            }
        } finally {
            ctx.close();
        }

        return results;
    }

    // ────────────────────────────────────────────────────────────────
    // 2. VÉRIFICATION SPAM AVEC RSPAMD
    // ────────────────────────────────────────────────────────────────

    /**
     * Analyse le score de spam via l'API HTTP de Rspamd
     */
    public SpamCheckResult checkSpamScore(String subject, String bodyHtml, String fromEmail) {
        log.info("📧 Vérification du score de spam avec Rspamd sur: {}", rspamdUrl);

        SpamCheckResult result = new SpamCheckResult();
        result.setSubject(subject);
        result.setFrom(fromEmail);

        try {
            String mimeMessage = buildMimeMessage(subject, bodyHtml, fromEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(mimeMessage, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    rspamdUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());

                double score = json.path("score").asDouble(0.0);
                double requiredScore = json.path("required_score").asDouble(5.0);
                String action = json.path("action").asText("no action");

                result.setScore(score);
                result.setRequiredScore(requiredScore);
                result.setAction(action);
                result.setPass(score < requiredScore);

                List<SpamCheckResult.Rule> rules = new ArrayList<>();
                JsonNode symbols = json.path("symbols");
                if (symbols.isObject()) {
                    symbols.fields().forEachRemaining(entry -> {
                        SpamCheckResult.Rule rule = new SpamCheckResult.Rule();
                        rule.setName(entry.getKey());
                        rule.setScore(entry.getValue().path("score").asDouble(0.0));
                        rule.setDescription(entry.getValue().path("description").asText(""));
                        rules.add(rule);
                    });
                }
                result.setRules(rules);

                log.info("✅ Spam check: score={}/{}, action={}, rules={}",
                        score, requiredScore, action, rules.size());

            } else {
                log.warn("⚠️ Rspamd a retourné un statut: {}", response.getStatusCode());
                result.setError("Erreur Rspamd: " + response.getStatusCode());
                result.setPass(false);
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'analyse Rspamd: {}", e.getMessage());
            result.setError(e.getMessage());
            result.setPass(false);
        }

        return result;
    }

    private String buildMimeMessage(String subject, String bodyHtml, String fromEmail) {
        return """
                MIME-Version: 1.0
                From: %s
                To: target-test@example.com
                Subject: %s
                Content-Type: text/html; charset=UTF-8

                %s
                """.formatted(fromEmail, subject, bodyHtml);
    }

    // ────────────────────────────────────────────────────────────────
    // 3. RAPPORT COMPLET DE DÉLIVRABILITÉ
    // ────────────────────────────────────────────────────────────────

    /**
     * Combine les analyses DNS et Anti-Spam pour générer un rapport global
     */
    public DeliverabilityReport getDeliverabilityReport(String domain, String subject, String bodyHtml, String fromEmail) {
        log.info("📊 Génération du rapport de délivrabilité pour: {}", domain);

        DeliverabilityReport report = new DeliverabilityReport();
        report.setDomain(domain);
        report.setFromEmail(fromEmail);
        report.setSubject(subject);

        // 1. Vérification DNS
        DomainCheckResult dnsResult = checkDomain(domain);
        report.setDnsCheck(dnsResult);

        // 2. Vérification Spam
        SpamCheckResult spamResult = checkSpamScore(subject, bodyHtml, fromEmail);
        report.setSpamCheck(spamResult);

        // 3. Score global sur 20
        int totalScore = dnsResult.getScore() + (int) (10 - Math.min(spamResult.getScore(), 10));
        report.setTotalScore(Math.max(0, totalScore));
        report.setPass(dnsResult.isPass() && spamResult.isPass());

        // 4. Generateur de Recommandations
        List<String> recommendations = new ArrayList<>();

        if (!dnsResult.isPass()) {
            Map<String, Object> checks = dnsResult.getChecks();
            if (checks != null) {
                if (!isCheckPassed(checks, "spf")) {
                    recommendations.add("Ajouter un enregistrement SPF pour ce domaine");
                }
                if (!isCheckPassed(checks, "dmarc")) {
                    recommendations.add("Ajouter un enregistrement DMARC pour ce domaine");
                }
                if (!isCheckPassed(checks, "dkim")) {
                    recommendations.add("Configurer DKIM pour ce domaine");
                }
            }
        }

        if (!spamResult.isPass()) {
            recommendations.add("Rédiger un contenu moins suspect (éviter les mots indicateurs de spam)");
            recommendations.add("Réduire le nombre de liens ou nettoyer les URLs dans le corps HTML");
        }

        report.setRecommendations(recommendations);

        log.info("✅ Rapport complet: pass={}, totalScore={}/20", report.isPass(), totalScore);
        return report;
    }

    private boolean isCheckPassed(Map<String, Object> checks, String key) {
        Object item = checks.get(key);
        if (item instanceof Map<?, ?> map) {
            return Boolean.TRUE.equals(map.get("pass"));
        }
        return false;
    }

    // ────────────────────────────────────────────────────────────────
    // CLASSES INTERNES (DTO)
    // ────────────────────────────────────────────────────────────────

    public static class DomainCheckResult {
        private String domain;
        private boolean pass;
        private int score;
        private Map<String, Object> checks;
        private String error;

        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public boolean isPass() { return pass; }
        public void setPass(boolean pass) { this.pass = pass; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public Map<String, Object> getChecks() { return checks; }
        public void setChecks(Map<String, Object> checks) { this.checks = checks; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class SpamCheckResult {
        private String subject;
        private String from;
        private double score;
        private double requiredScore = 5.0;
        private String action;
        private boolean pass;
        private List<Rule> rules = new ArrayList<>();
        private String error;

        public static class Rule {
            private String name;
            private double score;
            private String description;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public double getScore() { return score; }
            public void setScore(double score) { this.score = score; }
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
        }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public double getRequiredScore() { return requiredScore; }
        public void setRequiredScore(double requiredScore) { this.requiredScore = requiredScore; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public boolean isPass() { return pass; }
        public void setPass(boolean pass) { this.pass = pass; }
        public List<Rule> getRules() { return rules; }
        public void setRules(List<Rule> rules) { this.rules = rules; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class DeliverabilityReport {
        private String domain;
        private String fromEmail;
        private String subject;
        private DomainCheckResult dnsCheck;
        private SpamCheckResult spamCheck;
        private int totalScore;
        private boolean pass;
        private List<String> recommendations = new ArrayList<>();

        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getFromEmail() { return fromEmail; }
        public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public DomainCheckResult getDnsCheck() { return dnsCheck; }
        public void setDnsCheck(DomainCheckResult dnsCheck) { this.dnsCheck = dnsCheck; }
        public SpamCheckResult getSpamCheck() { return spamCheck; }
        public void setSpamCheck(SpamCheckResult spamCheck) { this.spamCheck = spamCheck; }
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
        public boolean isPass() { return pass; }
        public void setPass(boolean pass) { this.pass = pass; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}