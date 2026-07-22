CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    template_id UUID REFERENCES email_templates(id),
    target_group_id UUID REFERENCES target_groups(id),
    sender_email VARCHAR(255),
    status VARCHAR(50) DEFAULT 'DRAFT',
    authorized_by UUID REFERENCES operators(id),
    created_by UUID REFERENCES operators(id)
);