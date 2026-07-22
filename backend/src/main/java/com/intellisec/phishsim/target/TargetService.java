package com.intellisec.phishsim.target;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TargetService {

    private final TargetRepository targetRepository;
    private final TargetGroupRepository targetGroupRepository;

    public List<Target> getByGroup(UUID groupId) {
        return targetRepository.findByGroupId(groupId);
    }

    public Target save(Target target) {
        return targetRepository.save(target);
    }

    public void delete(UUID id) {
        targetRepository.deleteById(id);
    }

    // ✅ NOUVELLE METHODE : Import CSV
    public void importCsv(MultipartFile file, UUID groupId) throws Exception {
        // Vérifier que le groupe existe
        if (!targetGroupRepository.existsById(groupId)) {
            throw new RuntimeException("Groupe non trouvé");
        }

        List<Target> targets = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Ignorer l'en-tête (première ligne)
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Ignorer les lignes vides
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",");

                // Format: email,firstName,lastName
                if (columns.length >= 3) {
                    String email = columns[0].trim();
                    String firstName = columns[1].trim();
                    String lastName = columns[2].trim();

                    // Vérifier que l'email n'est pas vide
                    if (email.isEmpty()) {
                        continue;
                    }

                    Target target = new Target();
                    target.setEmail(email);
                    target.setFirstName(firstName);
                    target.setLastName(lastName);
                    target.setGroupId(groupId);

                    targets.add(target);
                }
            }
        }

        // Sauvegarder toutes les cibles
        targetRepository.saveAll(targets);
    }
}