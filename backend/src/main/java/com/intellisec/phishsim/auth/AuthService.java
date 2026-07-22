package com.intellisec.phishsim.auth;

import com.intellisec.phishsim.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MfaService mfaService;

    // ✅ LOGIN
    public AuthResponse login(String email, String password) {
        Operator operator = operatorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        if (!passwordEncoder.matches(password, operator.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        boolean mfaConfigured = operator.getTotpSecret() != null;
        return new AuthResponse(null, toDTO(operator), mfaConfigured);
    }

    // ✅ REGISTER : Version publique (force VIEWER)
    public AuthResponse register(String email, String password, String role,
                                 String firstName, String lastName,
                                 String phone, LocalDate birthDate) {
        return registerInternal(email, password, role, firstName, lastName, phone, birthDate, false, null);
    }

    // ✅ REGISTER : Version admin
    public AuthResponse registerAdmin(String email, String password, String role,
                                      String firstName, String lastName,
                                      String phone, LocalDate birthDate,
                                      String currentUserEmail) {
        return registerInternal(email, password, role, firstName, lastName, phone, birthDate, true, currentUserEmail);
    }

    // ✅ Méthode interne commune
    private AuthResponse registerInternal(String email, String password, String role,
                                          String firstName, String lastName,
                                          String phone, LocalDate birthDate,
                                          boolean isAdminCreation,
                                          String currentUserEmail) {

        // ✅ Si c'est une création par ADMIN, vérifier les droits
        if (isAdminCreation && currentUserEmail != null) {
            Operator currentUser = operatorRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // ✅ Si l'utilisateur actuel est ADMIN (pas SUPER_ADMIN) → il ne peut pas créer un autre ADMIN
            if (Operator.ROLE_ADMIN.equals(currentUser.getRole()) &&
                    Operator.ROLE_ADMIN.equals(role)) {
                throw new RuntimeException("❌ Un ADMIN ne peut pas créer un autre ADMIN. Seul un SUPER_ADMIN peut le faire.");
            }

            // ✅ Si l'utilisateur actuel est ADMIN (pas SUPER_ADMIN) → il ne peut pas créer un SUPER_ADMIN
            if (Operator.ROLE_ADMIN.equals(currentUser.getRole()) &&
                    Operator.ROLE_SUPER_ADMIN.equals(role)) {
                throw new RuntimeException("❌ Un ADMIN ne peut pas créer un SUPER_ADMIN.");
            }
        }

        Operator operator = new Operator();
        operator.setEmail(email);
        operator.setPasswordHash(passwordEncoder.encode(password));

        // ✅ Déterminer le rôle
        if (isAdminCreation) {
            operator.setRole(role);
        } else {
            operator.setRole(Operator.ROLE_VIEWER);
        }

        operator.setFirstName(firstName);
        operator.setLastName(lastName);
        operator.setPhone(phone);
        operator.setBirthDate(birthDate);
        operator.setCreatedAt(LocalDateTime.now());

        Operator saved = operatorRepository.save(operator);
        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole());
        return new AuthResponse(token, toDTO(saved), false);
    }

    // ✅ Supprimer un opérateur (avec vérification)
    public void deleteOperator(UUID operatorId, String currentUserEmail) {
        Operator currentUser = operatorRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Operator operatorToDelete = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("Opérateur non trouvé"));

        // ✅ Seul SUPER_ADMIN peut supprimer un ADMIN
        if (Operator.ROLE_ADMIN.equals(operatorToDelete.getRole())) {
            if (!Operator.ROLE_SUPER_ADMIN.equals(currentUser.getRole())) {
                throw new RuntimeException("❌ Seul un SUPER_ADMIN peut supprimer un ADMIN.");
            }
        }

        // ✅ Seul SUPER_ADMIN peut supprimer un SUPER_ADMIN
        if (Operator.ROLE_SUPER_ADMIN.equals(operatorToDelete.getRole())) {
            if (!Operator.ROLE_SUPER_ADMIN.equals(currentUser.getRole())) {
                throw new RuntimeException("❌ Seul un SUPER_ADMIN peut supprimer un SUPER_ADMIN.");
            }
        }

        // ✅ Ne pas se supprimer soi-même
        if (currentUser.getId().equals(operatorToDelete.getId())) {
            throw new RuntimeException("❌ Vous ne pouvez pas supprimer votre propre compte.");
        }

        operatorRepository.delete(operatorToDelete);
    }

    private OperatorDTO toDTO(Operator operator) {
        OperatorDTO dto = new OperatorDTO();
        dto.setId(operator.getId());
        dto.setEmail(operator.getEmail());
        dto.setRole(operator.getRole());
        dto.setFirstName(operator.getFirstName());
        dto.setLastName(operator.getLastName());
        dto.setPhone(operator.getPhone());
        dto.setBirthDate(operator.getBirthDate());
        dto.setCreatedAt(operator.getCreatedAt());
        return dto;
    }

    public String enableMfa(String email) {
        Operator operator = operatorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        String secret = mfaService.generateSecret();
        operator.setTotpSecret(secret);
        operatorRepository.save(operator);

        return mfaService.generateQrUrl(email, secret);
    }

    public AuthResponse verifyMfa(String email, int code) {
        Operator operator = operatorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        if (operator.getTotpSecret() == null) {
            throw new RuntimeException("MFA not enabled");
        }

        if (!mfaService.verifyCode(operator.getTotpSecret(), code)) {
            throw new RuntimeException("Invalid MFA code");
        }

        String token = jwtUtil.generateToken(operator.getEmail(), operator.getRole());
        return new AuthResponse(token, toDTO(operator));
    }

    public boolean isMfaConfigured(String email) {
        Operator operator = operatorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));
        return operator.getTotpSecret() != null;
    }

    // ✅ Récupérer tous les opérateurs
    public List<OperatorDTO> getAllOperators() {
        return operatorRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}