
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS landing_page_html TEXT;
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS red_flags TEXT;
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS sender_name TEXT;
ALTER TABLE ai_generation_logs ADD COLUMN IF NOT EXISTS sender_domain TEXT;


COMMENT ON COLUMN ai_generation_logs.landing_page_html IS 'Page de connexion + awareness générée par l''IA (Week 6)';
COMMENT ON COLUMN ai_generation_logs.red_flags IS 'Drapeaux rouges générés par l''IA (Week 6)';
COMMENT ON COLUMN ai_generation_logs.sender_name IS 'Nom de l''expéditeur fictif généré par l''IA (Week 6)';
COMMENT ON COLUMN ai_generation_logs.sender_domain IS 'Domaine fictif généré par l''IA (Week 6)';