package com.intellisec.phishsim.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    // ✅ VIEWER peut voir les logs
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    // ✅ VIEWER peut voir les stats
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalLogs", auditLogRepository.count());
        stats.put("totalSends", auditLogService.countSends());
        stats.put("sendsLast24h", auditLogService.countSendsSince(LocalDateTime.now().minusHours(24)));
        stats.put("sendsLast7Days", auditLogService.countSendsSince(LocalDateTime.now().minusDays(7)));
        stats.put("recentSends", auditLogService.getRecentSends(10));
        return ResponseEntity.ok(stats);
    }

    // ✅ VIEWER peut vérifier l'intégrité
    @GetMapping("/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> verifyIntegrity() {
        return ResponseEntity.ok(auditLogService.verifyIntegrity());
    }

    // ✅ VIEWER peut filtrer par action
    @GetMapping("/action")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AuditLog>> getLogsByAction(@RequestParam String action) {
        return ResponseEntity.ok(auditLogService.getLogsByAction(action));
    }

    // ✅ VIEWER peut filtrer par acteur
    @GetMapping("/actor")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<AuditLog>> getLogsByActor(@RequestParam String actor) {
        return ResponseEntity.ok(auditLogService.getLogsByActor(actor));
    }
}