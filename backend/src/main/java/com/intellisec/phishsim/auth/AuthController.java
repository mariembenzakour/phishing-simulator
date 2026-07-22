package com.intellisec.phishsim.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ✅ Inscription publique → VIEWER
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(
                request.getEmail(),
                request.getPassword(),
                "VIEWER",
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getBirthDate()
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

    // ✅ Création par ADMIN → Vérifier les droits
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/admin/create-operator")
    public ResponseEntity<AuthResponse> createOperator(@RequestBody RegisterRequest request,
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
                currentUserEmail
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
}