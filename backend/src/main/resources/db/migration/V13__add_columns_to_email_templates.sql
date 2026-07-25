-- Ajout des colonnes manquantes avec IF NOT EXISTS pour éviter les erreurs
ALTER TABLE email_templates ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;