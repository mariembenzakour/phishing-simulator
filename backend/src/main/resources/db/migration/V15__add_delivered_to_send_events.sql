-- Ajout de la colonne delivered pour suivre la délivrabilité des emails
ALTER TABLE send_events ADD COLUMN IF NOT EXISTS delivered BOOLEAN DEFAULT FALSE;