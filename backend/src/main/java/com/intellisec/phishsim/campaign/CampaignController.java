package com.intellisec.phishsim.campaign;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<Campaign>> getAll() {
        return ResponseEntity.ok(campaignService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Campaign> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Campaign> create(@RequestBody Campaign campaign) {
        return ResponseEntity.ok(campaignService.create(campaign));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Campaign> update(@PathVariable UUID id,
                                           @RequestBody Campaign campaign) {
        return ResponseEntity.ok(campaignService.update(id, campaign));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Campaign> clone(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.clone(id));
    }

    @PutMapping("/{id}/authorize")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Campaign> authorize(@PathVariable UUID id,
                                              @RequestParam UUID operatorId) {
        return ResponseEntity.ok(campaignService.authorize(id, operatorId));
    }

    @PutMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Campaign> pause(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.pause(id));
    }

    @PutMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<Campaign> resume(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.resume(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        campaignService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ NOUVEAU : Endpoint de délivrabilité appelé par le Angular
    @PostMapping("/check-deliverability")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    public ResponseEntity<CampaignService.DeliverabilityResponseDto> checkDeliverability(@RequestBody DeliverabilityRequest request) {
        return ResponseEntity.ok(campaignService.checkDeliverability(request));
    }

    // DTO de requête transmis par Angular
    @Data
    public static class DeliverabilityRequest {
        private String senderEmail;
        private String subject;
        private String bodyHtml;
        private String bodyText;
    }
}