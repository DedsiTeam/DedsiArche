-- 系统管理领域：截至 Flyway V7 的完整结构快照。
-- 用于阅读和结构参考，不属于 Flyway 迁移，不由应用自动执行。
-- 不要在已有业务库执行；新环境仍通过原 V1～V7 迁移初始化。

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

COMMENT ON TABLE sys_systems IS '受管理系统目录，维护编码、名称、入口地址、业务状态和排序';
COMMENT ON COLUMN sys_systems.id IS '系统唯一标识，26 位大写 ULID 字符串';
COMMENT ON COLUMN sys_systems.code IS '系统编码，全局唯一、区分大小写，创建后不可修改';
COMMENT ON COLUMN sys_systems.name IS '系统展示名称，最长 100 字符；更名同步关联权限的系统名称';
COMMENT ON COLUMN sys_systems.description IS '系统说明，可为空，最长 512 字符';
COMMENT ON COLUMN sys_systems.entry_url IS 'HTTP/HTTPS 绝对入口地址，最长 2048 字符，禁止包含账号密码；正常状态必填';
COMMENT ON COLUMN sys_systems.status IS '人工业务状态：NOT_LAUNCHED 未上线、NORMAL 正常、DISABLED 停用、UPDATING 更新中、MAINTENANCE 维护中、RETIRED 已退役；不代表健康检查或用户权限';
COMMENT ON COLUMN sys_systems.sort_order IS '展示排序，非负整数，默认 0，数值越小越靠前';
COMMENT ON COLUMN sys_systems.created_at IS '系统登记时间，北京时间（Asia/Shanghai）本地值';
COMMENT ON COLUMN sys_systems.updated_at IS '系统最后修改时间，北京时间（Asia/Shanghai）本地值';
COMMENT ON COLUMN sys_systems.version IS 'JPA 乐观锁版本，编辑和删除时校验，防止并发覆盖';
