-- Activer l'extension pgcrypto pour le chiffrement
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Table pour stocker les clés de chiffrement (optionnel)
CREATE TABLE IF NOT EXISTS encryption_keys (
                                               id VARCHAR(50) PRIMARY KEY,
    key_value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Insérer la clé de chiffrement par défaut
INSERT INTO encryption_keys (id, key_value)
VALUES ('totp_encryption_key', 'phishsim-encryption-key-2026')
    ON CONFLICT (id) DO NOTHING;