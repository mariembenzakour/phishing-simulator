package com.intellisec.phishsim.auth;

import com.intellisec.phishsim.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MfaService mfaService;

    // ✅ Dossier de stockage des avatars + préfixe de l'URL exposée au front
    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";
    private static final String AVATAR_URL_PREFIX = "/api/auth/avatars/";
    private static final List<String> ALLOWED_AVATAR_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5 Mo

    // ✅ LOGIN
    public AuthResponse login(String email, String password) {
        Operator operator = operatorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        if (!passwordEncoder.matches(password, operator.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        // ✅ GÉNÉRER LE TOKEN
        String token = jwtUtil.generateToken(operator.getEmail(), operator.getRole(), operator.getId());

        log.info("✅ Token généré pour {}: {}...", email, token.substring(0, 20));

        boolean mfaConfigured = operator.getTotpSecret() != null;
        return new AuthResponse(token, toDTO(operator), mfaConfigured);
    }

    // ✅ REGISTER : Version publique (force VIEWER), avec avatar optionnel
    public AuthResponse register(String email, String password, String role,
                                 String firstName, String lastName,
                                 String phone, LocalDate birthDate, MultipartFile avatar) {
        return registerInternal(email, password, role, firstName, lastName, phone, birthDate, false, null, avatar);
    }

    // ✅ REGISTER : Version admin, avec avatar optionnel
    public AuthResponse registerAdmin(String email, String password, String role,
                                      String firstName, String lastName,
                                      String phone, LocalDate birthDate,
                                      String currentUserEmail, MultipartFile avatar) {
        return registerInternal(email, password, role, firstName, lastName, phone, birthDate, true, currentUserEmail, avatar);
    }

    // ✅ Méthode interne commune
    private AuthResponse registerInternal(String email, String password, String role,
                                          String firstName, String lastName,
                                          String phone, LocalDate birthDate,
                                          boolean isAdminCreation,
                                          String currentUserEmail,
                                          MultipartFile avatar) {

        if (isAdminCreation && currentUserEmail != null) {
            Operator currentUser = operatorRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (Operator.ROLE_ADMIN.equals(currentUser.getRole()) &&
                    Operator.ROLE_ADMIN.equals(role)) {
                throw new RuntimeException("❌ Un ADMIN ne peut pas créer un autre ADMIN. Seul un SUPER_ADMIN peut le faire.");
            }

            if (Operator.ROLE_ADMIN.equals(currentUser.getRole()) &&
                    Operator.ROLE_SUPER_ADMIN.equals(role)) {
                throw new RuntimeException("❌ Un ADMIN ne peut pas créer un SUPER_ADMIN.");
            }
        }

        // ✅ Stocker l'avatar (si fourni) avant de créer l'opérateur
        String avatarUrl = storeAvatar(avatar);

        Operator operator = new Operator();
        operator.setEmail(email);
        operator.setPasswordHash(passwordEncoder.encode(password));

        if (isAdminCreation) {
            operator.setRole(role);
        } else {
            operator.setRole(Operator.ROLE_VIEWER);
        }

        operator.setFirstName(firstName);
        operator.setLastName(lastName);
        operator.setPhone(phone);
        operator.setBirthDate(birthDate);
        operator.setAvatar(avatarUrl);
        operator.setCreatedAt(LocalDateTime.now());

        Operator saved = operatorRepository.save(operator);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole(), saved.getId());
        return new AuthResponse(token, toDTO(saved), false);
    }

    // ✅ Valide et enregistre le fichier avatar sur le disque, retourne l'URL relative à stocker en BD
    private String storeAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return null;
        }

        if (!ALLOWED_AVATAR_TYPES.contains(avatar.getContentType())) {
            throw new RuntimeException("❌ Format d'image non supporté (JPEG, PNG ou WEBP uniquement).");
        }

        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new RuntimeException("❌ L'image ne doit pas dépasser 5 Mo.");
        }

        try {
            Path uploadPath = Paths.get(AVATAR_UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = avatar.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String filename = UUID.randomUUID() + extension;

            Path targetPath = uploadPath.resolve(filename);
            Files.copy(avatar.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return AVATAR_URL_PREFIX + filename;
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement de l'avatar", e);
            throw new RuntimeException("❌ Erreur lors de l'enregistrement de la photo de profil.");
        }
    }

    // ✅ Résout le chemin disque d'un avatar à partir de son nom de fichier (pour le servir)
    public Path getAvatarPath(String filename) {
        return Paths.get(AVATAR_UPLOAD_DIR).resolve(filename).normalize();
    }

    // ✅ NOUVEAU : Profil complet de l'utilisateur connecté (utilisé par GET /api/auth/me)
    public OperatorDTO getCurrentUser(String email) {
        Operator operator = operatorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));
        return toDTO(operator);
    }

    // ✅ Supprimer un opérateur
    public void deleteOperator(UUID operatorId, String currentUserEmail) {
        Operator currentUser = operatorRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Operator operatorToDelete = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("Opérateur non trouvé"));

        if (Operator.ROLE_ADMIN.equals(operatorToDelete.getRole())) {
            if (!Operator.ROLE_SUPER_ADMIN.equals(currentUser.getRole())) {
                throw new RuntimeException("❌ Seul un SUPER_ADMIN peut supprimer un ADMIN.");
            }
        }

        if (Operator.ROLE_SUPER_ADMIN.equals(operatorToDelete.getRole())) {
            if (!Operator.ROLE_SUPER_ADMIN.equals(currentUser.getRole())) {
                throw new RuntimeException("❌ Seul un SUPER_ADMIN peut supprimer un SUPER_ADMIN.");
            }
        }

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
        dto.setAvatar(operator.getAvatar());
        dto.setCreatedAt(operator.getCreatedAt());
        return dto;
    }

    // ✅ Activer le MFA avec chiffrement du secret
    public String enableMfa(String email) {
        Operator operator = operatorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        String secret = mfaService.generateSecret();
        operator.setEncryptedTotpSecret(secret);
        operatorRepository.save(operator);

        return mfaService.generateQrUrl(email, secret);
    }

    // ✅ Vérifier le MFA avec déchiffrement du secret
    public AuthResponse verifyMfa(String email, int code) {
        Operator operator = operatorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        if (operator.getTotpSecret() == null) {
            throw new RuntimeException("MFA not enabled");
        }

        String decryptedSecret = operator.getDecryptedTotpSecret();

        if (decryptedSecret == null || decryptedSecret.isEmpty()) {
            throw new RuntimeException("MFA not properly configured");
        }

        if (!mfaService.verifyCode(decryptedSecret, code)) {
            throw new RuntimeException("Invalid MFA code");
        }

        String token = jwtUtil.generateToken(operator.getEmail(), operator.getRole(), operator.getId());
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