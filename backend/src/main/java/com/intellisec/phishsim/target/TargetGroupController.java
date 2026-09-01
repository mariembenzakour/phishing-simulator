package com.intellisec.phishsim.target;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class TargetGroupController {

    private final TargetGroupService targetGroupService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<TargetGroup>> getAll() {
        return ResponseEntity.ok(targetGroupService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<TargetGroup> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(targetGroupService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<TargetGroup> create(@RequestBody TargetGroup group) {
        return ResponseEntity.ok(targetGroupService.create(group));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<TargetGroup> update(@PathVariable UUID id, @RequestBody TargetGroup group) {
        return ResponseEntity.ok(targetGroupService.update(id, group));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        targetGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}