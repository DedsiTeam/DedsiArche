CREATE TABLE sys_users (
    id uuid PRIMARY KEY,
    username varchar(50) NOT NULL,
    display_name varchar(100) NOT NULL,
    password_hash varchar(100) NOT NULL,
    enabled boolean NOT NULL,
    created_at timestamp with time zone NOT NULL,
    version bigint NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
);
CREATE TABLE sys_refresh_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES sys_users(id),
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamp with time zone NOT NULL,
    revoked boolean NOT NULL
);
CREATE INDEX ix_refresh_sessions_user ON sys_refresh_sessions(user_id);
CREATE INDEX ix_refresh_sessions_expiry ON sys_refresh_sessions(expires_at);
