ALTER TABLE sys_users ADD COLUMN credential_version bigint NOT NULL DEFAULT 0;
ALTER TABLE sys_refresh_sessions ADD COLUMN credential_version bigint NOT NULL DEFAULT 0;
