-- ✅ Ajout de la colonne awareness_page_html
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS awareness_page_html TEXT;

COMMENT ON COLUMN ai_generation_logs.awareness_page_html IS 'Page de sensibilisation générée par l''IA (Week 6) - séparée de la landing page';