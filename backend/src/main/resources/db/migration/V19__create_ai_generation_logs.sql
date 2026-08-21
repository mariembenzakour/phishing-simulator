
CREATE TABLE IF NOT EXISTS ai_generation_logs (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario VARCHAR(255) NOT NULL,
    language VARCHAR(10),
    prompt TEXT,
    generated_subject TEXT,
    generated_body TEXT,
    generated_by VARCHAR(255) NOT NULL,
    approved_by VARCHAR(255),
    approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP
    );

-- Index pour les recherches
CREATE INDEX IF NOT EXISTS idx_ai_logs_generated_by ON ai_generation_logs(generated_by);
CREATE INDEX IF NOT EXISTS idx_ai_logs_approved ON ai_generation_logs(approved);
CREATE INDEX IF NOT EXISTS idx_ai_logs_scenario ON ai_generation_logs(scenario);