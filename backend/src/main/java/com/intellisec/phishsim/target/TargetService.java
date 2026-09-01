package com.intellisec.phishsim.target;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public void importCsv(MultipartFile file, UUID groupId) throws Exception {
        if (!targetGroupRepository.existsById(groupId)) {
            throw new RuntimeException("Groupe non trouvé");
        }

        List<Target> targets = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",");

                if (columns.length >= 3) {
                    String email = columns[0].trim();
                    String firstName = columns[1].trim();
                    String lastName = columns[2].trim();

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

        if (!targets.isEmpty()) {
            targetRepository.saveAll(targets);
        }
    }
}