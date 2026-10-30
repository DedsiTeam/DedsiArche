CREATE TABLE sys_systems (
    id VARCHAR(26) PRIMARY KEY CHECK (id ~ '^[0-7][0-9A-HJKMNP-TV-Z]{25}$'),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(512),
    entry_url VARCHAR(2048),
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_LAUNCHED',
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_systems_code UNIQUE (code),
    CONSTRAINT ck_systems_status CHECK (
        status IN ('NOT_LAUNCHED', 'NORMAL', 'DISABLED', 'UPDATING', 'MAINTENANCE', 'RETIRED')
    ),
    CONSTRAINT ck_systems_normal_entry CHECK (status <> 'NORMAL' OR entry_url IS NOT NULL)
);
CREATE INDEX ix_systems_sort ON sys_systems (sort_order, id);
