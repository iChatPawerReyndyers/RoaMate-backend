-- Real username/password accounts, replacing the anonymous per-install
-- device id as the identity behind userId (see AuthController). The rest
-- of the schema already stores userId as a plain VARCHAR everywhere - no
-- foreign key to this table exists or is added here, since existing rows
-- already hold anonymous device ids that predate any User row. Migrating
-- a given anonymous id's data onto a newly-registered account's id is
-- handled in application code (UserService#migrateAnonymousData) rather
-- than at the schema level.
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);
