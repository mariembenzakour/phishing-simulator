CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE operators (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           email VARCHAR(255) UNIQUE NOT NULL,
                           password_hash VARCHAR(255) NOT NULL,
                           totp_secret VARCHAR(255),
                           mfa_enabled BOOLEAN DEFAULT FALSE,
                           role VARCHAR(50) NOT NULL DEFAULT 'OPERATOR',
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           last_login TIMESTAMP
);