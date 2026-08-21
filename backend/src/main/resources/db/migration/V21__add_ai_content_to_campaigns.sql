-- ✅ Ajout des champs pour le contenu IA dans les campagnes
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS ai_generation_id UUID;
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS custom_subject TEXT;
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS custom_body_html TEXT;
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS custom_body_text TEXT;

-- ✅ Index pour les recherches
CREATE INDEX IF NOT EXISTS idx_campaigns_ai_generation_id ON campaigns(ai_generation_id);

-- ✅ Commentaires
COMMENT ON COLUMN campaigns.ai_generation_id IS 'Référence au draft IA approuvé (AiGenerationLog)';
COMMENT ON COLUMN campaigns.custom_subject IS 'Sujet de l''email généré par l''IA';
COMMENT ON COLUMN campaigns.custom_body_html IS 'Corps HTML de l''email généré par l''IA';
COMMENT ON COLUMN campaigns.custom_body_text IS 'Corps texte de l''email généré par l''IA';