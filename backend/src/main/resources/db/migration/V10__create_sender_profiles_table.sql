CREATE TABLE sender_profiles (
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 name VARCHAR(255) NOT NULL,
                                 from_name VARCHAR(255) NOT NULL,
                                 from_email VARCHAR(255) NOT NULL,
                                 reply_to VARCHAR(255),
                                 created_by UUID REFERENCES operators(id),
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);