-- 1. Ajouter une colonne temporaire pour le secret chiffré (en VARCHAR)
ALTER TABLE operators ADD COLUMN totp_secret_encrypted VARCHAR(255);

-- 2. Les données existantes en clair seront perdues
-- On laisse vide et on régénère les secrets
UPDATE operators SET totp_secret_encrypted = NULL;

-- 3. Supprimer l'ancienne colonne en clair
ALTER TABLE operators DROP COLUMN totp_secret;

-- 4. Renommer la nouvelle colonne
ALTER TABLE operators RENAME COLUMN totp_secret_encrypted TO totp_secret;