package com.intellisec.phishsim.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ✅ Inscription publique → VIEWER (avec upload d'avatar optionnel)
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthResponse> register(@RequestPart("data") RegisterRequest request,
                                                 @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        return ResponseEntity.ok(authService.register(
                request.getEmail(),
                request.getPassword(),
                "VIEWER",
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getBirthDate(),
                avatar
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(
                request.getEmail(),
                request.getPassword()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<String> enableMfa(@RequestParam String email) {
        return ResponseEntity.ok(authService.enableMfa(email));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthResponse> verifyMfa(@RequestParam String email,
                                                  @RequestParam int code) {
        return ResponseEntity.ok(authService.verifyMfa(email, code));
    }

    // ✅ Création par ADMIN → Vérifier les droits (avec upload d'avatar optionnel)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping(value = "/admin/create-operator", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthResponse> createOperator(@RequestPart("data") RegisterRequest request,
                                                       @RequestPart(value = "avatar", required = false) MultipartFile avatar,
                                                       Authentication authentication) {
        String currentUserEmail = authentication.getName();
        return ResponseEntity.ok(authService.registerAdmin(
                request.getEmail(),
                request.getPassword(),
                request.getRole(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getBirthDate(),
                currentUserEmail,
                avatar
        ));
    }

    // ✅ NOUVEAU : Supprimer un opérateur (uniquement ADMIN et SUPER_ADMIN)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/admin/delete-operator/{id}")
    public ResponseEntity<Void> deleteOperator(@PathVariable UUID id,
                                               Authentication authentication) {
        String currentUserEmail = authentication.getName();
        authService.deleteOperator(id, currentUserEmail);
        return ResponseEntity.noContent().build();
    }

    // ✅ NOUVEAU : Lister tous les opérateurs
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'OPERATOR')")
    @GetMapping("/admin/operators")
    public ResponseEntity<List<OperatorDTO>> getAllOperators() {
        return ResponseEntity.ok(authService.getAllOperators());
    }

    // ✅ NOUVEAU : Profil complet de l'utilisateur actuellement connecté (email, rôle, avatar...)
    // Utilisé par le front (ex: sidebar) pour afficher des infos absentes du JWT, comme l'avatar.
    @GetMapping("/me")
    public ResponseEntity<OperatorDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }

    // ✅ NOUVEAU : Servir les photos de profil uploadées
    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        Path filePath = authService.getAvatarPath(filename);

        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = null;
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}