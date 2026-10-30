CREATE TABLE sys_permissions (
    id VARCHAR(26) PRIMARY KEY CHECK (id ~ '^[0-7][0-9A-HJKMNP-TV-Z]{25}$'),
    system_code VARCHAR(100) NOT NULL,
    system_name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_permissions_system_code UNIQUE (system_code, code)
);
CREATE INDEX ix_permissions_system_sort ON sys_permissions (system_code, sort_order, id);
