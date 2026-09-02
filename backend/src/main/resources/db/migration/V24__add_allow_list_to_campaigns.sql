-- ✅ Ajout de la colonne allow_list
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS allow_list TEXT;

COMMENT ON COLUMN campaigns.allow_list IS 'Liste d''emails autorisés ';