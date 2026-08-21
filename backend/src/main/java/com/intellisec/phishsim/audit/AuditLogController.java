package com.intellisec.phishsim.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import  org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @GetMapping("/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyIntegrity() {
        return ResponseEntity.ok(auditLogService.verifyIntegrity());
    }

    @GetMapping("/action")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<AuditLog>> getLogsByAction(@RequestParam String action) {
        return ResponseEntity.ok(auditLogService.getLogsByAction(action));
    }

    @GetMapping("/actor")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<AuditLog>> getLogsByActor(@RequestParam String actor) {
        return ResponseEntity.ok(auditLogService.getLogsByActor(actor));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalLogs", auditLogRepository.count());
        stats.put("totalSends", auditLogService.countSends());
        stats.put("sendsLast24h", auditLogService.countSendsSince(LocalDateTime.now().minusHours(24)));
        stats.put("sendsLast7Days", auditLogService.countSendsSince(LocalDateTime.now().minusDays(7)));
        stats.put("recentSends", auditLogService.getRecentSends(10));
        return ResponseEntity.ok(stats);
    }
}