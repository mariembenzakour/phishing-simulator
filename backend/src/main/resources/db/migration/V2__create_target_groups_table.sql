CREATE TABLE target_groups (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               name VARCHAR(255) NOT NULL,
                               created_by UUID REFERENCES operators(id)
);