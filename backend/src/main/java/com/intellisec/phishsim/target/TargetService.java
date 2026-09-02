package com.intellisec.phishsim.target;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TargetService {

    private final TargetRepository targetRepository;
    private final TargetGroupRepository targetGroupRepository;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public List<Target> getByGroup(UUID groupId) {
        return targetRepository.findByGroupId(groupId);
    }

    public Target getById(UUID id) {
        return targetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Target not found"));
    }

    @Transactional
    public Target save(Target target) {
        return targetRepository.save(target);
    }

    @Transactional
    public Target update(UUID id, Target targetData) {
        Target target = getById(id);
        target.setEmail(targetData.getEmail());
        target.setFirstName(targetData.getFirstName());
        target.setLastName(targetData.getLastName());
        target.setGroupId(targetData.getGroupId());
        return targetRepository.save(target);
    }

    @Transactional
    public void delete(UUID id) {
        Target target = getById(id);
        targetRepository.delete(target);
    }

    /**
     * ✅ Import CSV avec validation, déduplication et reporting
     */
    @Transactional
    public ImportResult importCsv(MultipartFile file, UUID groupId) throws Exception {
        if (!targetGroupRepository.existsById(groupId)) {
            throw new RuntimeException("Groupe non trouvé");
        }

        List<Target> validTargets = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> uniqueEmailsInFile = new HashSet<>();
        Set<String> duplicateEmailsInFile = new HashSet<>();

        // ✅ Récupérer les emails existants en BDD pour ce groupe
        Set<String> existingEmails = new HashSet<>();
        for (Target existing : targetRepository.findByGroupId(groupId)) {
            existingEmails.add(existing.getEmail().toLowerCase());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Ignorer l'en-tête
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Ignorer les lignes vides
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",", -1);

                // ✅ Vérifier le nombre de colonnes
                if (columns.length < 3) {
                    errors.add("Ligne " + lineNumber + ": Format invalide (3 colonnes attendues : email,firstName,lastName)");
                    continue;
                }

                String email = columns[0].trim();
                String firstName = columns[1].trim();
                String lastName = columns[2].trim();

                // ✅ Valider l'email (non vide)
                if (email.isEmpty()) {
                    errors.add("Ligne " + lineNumber + ": Email vide");
                    continue;
                }

                // ✅ Valider le format de l'email
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    errors.add("Ligne " + lineNumber + ": Email invalide '" + email + "'");
                    continue;
                }

                String emailLower = email.toLowerCase();

                // ✅ Déduplication intra-fichier (doublons dans le CSV)
                if (uniqueEmailsInFile.contains(emailLower)) {
                    duplicateEmailsInFile.add(emailLower);
                    errors.add("Ligne " + lineNumber + ": Email dupliqué dans le fichier '" + email + "'");
                    continue;
                }

                // ✅ Déduplication avec BDD (email déjà existant)
                if (existingEmails.contains(emailLower)) {
                    errors.add("Ligne " + lineNumber + ": Email déjà existant en base '" + email + "'");
                    continue;
                }

                // ✅ Ajouter à la liste des emails du fichier
                uniqueEmailsInFile.add(emailLower);

                // ✅ Créer la cible
                Target target = new Target();
                target.setEmail(email);
                target.setFirstName(firstName);
                target.setLastName(lastName);
                target.setGroupId(groupId);
                validTargets.add(target);
            }
        }

        // ✅ Sauvegarder les cibles valides
        if (!validTargets.isEmpty()) {
            targetRepository.saveAll(validTargets);
        }

        return new ImportResult(
                validTargets.size(),
                errors.size(),
                errors,
                validTargets,
                new ArrayList<>(duplicateEmailsInFile)
        );
    }

    /**
     * ✅ Résultat de l'import CSV
     */
    public static class ImportResult {
        private final int importedCount;
        private final int errorCount;
        private final List<String> errors;
        private final List<Target> importedTargets;
        private final List<String> duplicateEmails;

        public ImportResult(int importedCount, int errorCount, List<String> errors,
                            List<Target> importedTargets, List<String> duplicateEmails) {
            this.importedCount = importedCount;
            this.errorCount = errorCount;
            this.errors = errors;
            this.importedTargets = importedTargets;
            this.duplicateEmails = duplicateEmails;
        }

        public int getImportedCount() {
            return importedCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<Target> getImportedTargets() {
            return importedTargets;
        }

        public List<String> getDuplicateEmails() {
            return duplicateEmails;
        }

        public boolean hasErrors() {
            return errorCount > 0;
        }

        public boolean hasImports() {
            return importedCount > 0;
        }

        public String getSummary() {
            return String.format("✅ %d cible(s) importée(s) | ⚠️ %d erreur(s)", importedCount, errorCount);
        }
    }
}