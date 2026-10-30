-- 权限领域：截至 Flyway V7 的完整结构快照。
-- 用于阅读和结构参考，不属于 Flyway 迁移，不由应用自动执行。
-- 不要在已有业务库执行；新环境仍通过原 V1～V7 迁移初始化。

CREATE TABLE sys_permissions (
    id VARCHAR(26) PRIMARY KEY CHECK (id ~ '^[0-7][0-9A-HJKMNP-TV-Z]{25}$'),
    system_code VARCHAR(100) NOT NULL,
    system_name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_permissions_system_code UNIQUE (system_code, code)
);
CREATE INDEX ix_permissions_system_sort ON sys_permissions (system_code, sort_order, id);

COMMENT ON TABLE sys_permissions IS '权限定义，维护所属系统、权限编码、展示资料及启用状态';
COMMENT ON COLUMN sys_permissions.id IS '权限唯一标识，26 位大写 ULID 字符串';
COMMENT ON COLUMN sys_permissions.system_code IS '所属系统编码，对应 sys_systems.code；与权限编码联合唯一，创建后不可修改';
COMMENT ON COLUMN sys_permissions.system_name IS '所属系统的冗余展示名称，由服务端读取，系统更名时同步';
COMMENT ON COLUMN sys_permissions.code IS '权限编码，在同一系统内唯一，创建后不可修改';
COMMENT ON COLUMN sys_permissions.name IS '权限展示名称';
COMMENT ON COLUMN sys_permissions.description IS '权限说明，可为空，最长 500 字符';
COMMENT ON COLUMN sys_permissions.enabled IS '权限定义是否启用，默认 true；当前不直接承担用户授权判定';
COMMENT ON COLUMN sys_permissions.sort_order IS '展示排序，非负整数，默认 0，数值越小越靠前';
COMMENT ON COLUMN sys_permissions.created_at IS '权限创建时间，北京时间（Asia/Shanghai）本地值';
COMMENT ON COLUMN sys_permissions.updated_at IS '权限最后修改时间，北京时间（Asia/Shanghai）本地值';
COMMENT ON COLUMN sys_permissions.version IS '乐观锁版本，编辑或删除时校验；系统名称同步也会递增';
