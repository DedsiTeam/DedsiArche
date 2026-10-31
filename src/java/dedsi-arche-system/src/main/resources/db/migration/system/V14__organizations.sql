-- 组织节点独立聚合；父子通过同系统复合外键连接，禁止级联删除子树。
CREATE TABLE sys_organizations (
    id varchar(26) PRIMARY KEY,
    system_code varchar(100) NOT NULL,
    system_name varchar(100) NOT NULL,
    parent_id varchar(26),
    code varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    name1 varchar(100),
    name2 varchar(100),
    name3 varchar(100),
    name4 varchar(100),
    sort_order integer NOT NULL DEFAULT 0,
    description varchar(500),
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_organizations_system_code UNIQUE (system_code, code),
    CONSTRAINT uk_organizations_system_id UNIQUE (system_code, id),
    CONSTRAINT fk_organizations_system FOREIGN KEY (system_code) REFERENCES sys_systems(code) ON DELETE RESTRICT,
    CONSTRAINT fk_organizations_parent FOREIGN KEY (system_code, parent_id)
        REFERENCES sys_organizations(system_code, id) ON DELETE RESTRICT,
    CONSTRAINT ck_organizations_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_organizations_not_self CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE INDEX idx_organizations_parent ON sys_organizations(parent_id);
CREATE INDEX idx_organizations_tree ON sys_organizations(system_code, parent_id, sort_order, code, id);

COMMENT ON TABLE sys_organizations IS '组织机构独立节点聚合；系统内构成树，移动节点保留子树，仅允许删除叶子';
COMMENT ON COLUMN sys_organizations.id IS '组织节点 ULID 主键，26 位大写字符串';
COMMENT ON COLUMN sys_organizations.system_code IS '所属系统编码，不可变，外键引用系统编码';
COMMENT ON COLUMN sys_organizations.system_name IS '所属系统名称，系统改名时同事务同步并推进组织版本';
COMMENT ON COLUMN sys_organizations.parent_id IS '同系统上级组织 ULID 外键，空值表示顶级组织';
COMMENT ON COLUMN sys_organizations.code IS '系统内唯一的组织编码，创建后不可修改';
COMMENT ON COLUMN sys_organizations.name IS '组织名称，最多 100 字符';
COMMENT ON COLUMN sys_organizations.name1 IS '备用名称 1，可空，最多 100 字符';
COMMENT ON COLUMN sys_organizations.name2 IS '备用名称 2，可空，最多 100 字符';
COMMENT ON COLUMN sys_organizations.name3 IS '备用名称 3，可空，最多 100 字符';
COMMENT ON COLUMN sys_organizations.name4 IS '备用名称 4，可空，最多 100 字符';
COMMENT ON COLUMN sys_organizations.sort_order IS '同层排序，非负整数，默认 0';
COMMENT ON COLUMN sys_organizations.description IS '组织说明，可空，最多 500 字符';
COMMENT ON COLUMN sys_organizations.enabled IS '是否启用，默认启用，启用节点的上级必须启用';
COMMENT ON COLUMN sys_organizations.created_at IS '创建时间，北京时间 Asia/Shanghai';
COMMENT ON COLUMN sys_organizations.updated_at IS '最后修改时间，北京时间 Asia/Shanghai';
COMMENT ON COLUMN sys_organizations.version IS '聚合乐观锁版本，实际变化自动递增';
