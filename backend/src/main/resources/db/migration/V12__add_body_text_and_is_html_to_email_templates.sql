-- Ajout des colonnes body_text et is_html à la table email_templates
ALTER TABLE email_templates ADD COLUMN body_text TEXT;
ALTER TABLE email_templates ADD COLUMN is_html BOOLEAN DEFAULT TRUE;