-- ✅ Ajout de la colonne body_text manquante dans la table ai_generation_logs
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS body_text TEXT;