package com.intellisec.phishsim.target;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class TargetController {

    private final TargetService targetService;

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<Target>> getByGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(targetService.getByGroup(groupId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Target> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(targetService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Target> create(@RequestBody Target target) {
        return ResponseEntity.ok(targetService.save(target));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Target> update(@PathVariable UUID id, @RequestBody Target target) {
        return ResponseEntity.ok(targetService.update(id, target));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        targetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * ✅ Import CSV avec validation, déduplication et reporting
     * Retourne un résultat détaillé avec le nombre d'imports et les erreurs
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<TargetService.ImportResult> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("groupId") UUID groupId) {
        try {
            TargetService.ImportResult result = targetService.importCsv(file, groupId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Retourner un résultat d'erreur
            List<String> errors = List.of("Erreur lors de l'import: " + e.getMessage());
            TargetService.ImportResult errorResult = new TargetService.ImportResult(
                    0,
                    1,
                    errors,
                    List.of(),
                    List.of()
            );
            return ResponseEntity.badRequest().body(errorResult);
        }
    }
}