package com.intellisec.phishsim.email;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sender-profiles")
@RequiredArgsConstructor
public class SenderProfileController {

    private final SenderProfileRepository senderProfileRepository;

    // ✅ GET ALL (Appelé par getAll() dans Angular)
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<SenderProfile>> getAll() {
        return ResponseEntity.ok(senderProfileRepository.findAll());
    }

    // ✅ GET BY ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<SenderProfile> getById(@PathVariable UUID id) {
        return senderProfileRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ CREATE (Appelé par create() dans Angular)
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<SenderProfile> create(@RequestBody SenderProfile senderProfile) {
        return ResponseEntity.ok(senderProfileRepository.save(senderProfile));
    }

    // ✅ UPDATE (Appelé par update() dans Angular)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<SenderProfile> update(@PathVariable UUID id, @RequestBody SenderProfile updatedProfile) {
        return senderProfileRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedProfile.getName());
                    existing.setFromName(updatedProfile.getFromName());
                    existing.setFromEmail(updatedProfile.getFromEmail());
                    existing.setReplyTo(updatedProfile.getReplyTo());
                    return ResponseEntity.ok(senderProfileRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ DELETE (Appelé par delete() dans Angular)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        senderProfileRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}