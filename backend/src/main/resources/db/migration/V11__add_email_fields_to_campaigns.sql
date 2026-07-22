ALTER TABLE campaigns
    ADD COLUMN sender_profile_id UUID REFERENCES sender_profiles(id),
ADD COLUMN dry_run BOOLEAN DEFAULT FALSE,
ADD COLUMN dry_run_email VARCHAR(255),
ADD COLUMN throttle_seconds INTEGER DEFAULT 5;