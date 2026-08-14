package com.intellisec.phishsim.common.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 10000;
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;

    // ✅ VALEURS PAR DÉFAUT
    private static final String DEFAULT_PASSWORD = "phishsim-encryption-key-2026";
    private static final String DEFAULT_SALT = "phishsim-salt-2026";

    @Value("${app.encryption.password:phishsim-encryption-key-2026}")
    private String password;

    @Value("${app.encryption.salt:phishsim-salt-2026}")
    private String salt;

    @PostConstruct
    public void init() {
        log.info("🔑 EncryptionUtil initialisé - password: {}, salt: {}",
                password != null ? "OK" : "NULL",
                salt != null ? "OK" : "NULL");
    }

    /**
     * Chiffrer une chaîne de caractères (retourne une chaîne Base64)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // Générer un IV aléatoire
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Créer la clé de chiffrement
            SecretKey secretKey = generateKey();

            // Initialiser le chiffreur
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            // Chiffrer
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combiner IV + données chiffrées
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            // Encoder en Base64 pour stockage dans VARCHAR
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("❌ Erreur lors du chiffrement : {}", e.getMessage());
            throw new RuntimeException("Erreur de chiffrement", e);
        }
    }

    /**
     * Déchiffrer une chaîne de caractères (depuis Base64)
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // Extraire l'IV (16 premiers octets)
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Extraire les données chiffrées
            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            // Créer la clé de déchiffrement
            SecretKey secretKey = generateKey();

            // Initialiser le déchiffreur
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // Déchiffrer
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("❌ Erreur lors du déchiffrement : {}", e.getMessage());
            // ✅ Fallback : retourner le texte en clair si le déchiffrement échoue
            return encryptedText;
        }
    }

    /**
     * Générer la clé de chiffrement à partir du mot de passe et du sel
     */
    private SecretKey generateKey() throws Exception {
        // ✅ Utiliser les valeurs par défaut si les propriétés sont null
        String pwd = (password != null && !password.isEmpty()) ? password : DEFAULT_PASSWORD;
        String slt = (salt != null && !salt.isEmpty()) ? salt : DEFAULT_SALT;

        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(
                pwd.toCharArray(),
                slt.getBytes(StandardCharsets.UTF_8),
                ITERATION_COUNT,
                KEY_LENGTH
        );
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}