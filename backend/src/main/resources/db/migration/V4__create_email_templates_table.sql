CREATE TABLE email_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body_html TEXT,
    status VARCHAR(50) DEFAULT 'DRAFT',
    approved_by UUID REFERENCES operators(id)
);