CREATE TABLE sys_positions (
    id varchar(26) PRIMARY KEY,
    code varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(500),
    enabled boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_positions_code UNIQUE (code),
    CONSTRAINT ck_positions_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE sys_position_permissions (
    id varchar(26) PRIMARY KEY,
    position_id varchar(26) NOT NULL,
    permission_id varchar(26) NOT NULL,
    system_code varchar(100) NOT NULL,
    system_name varchar(100) NOT NULL,
    permission_code varchar(100) NOT NULL,
    permission_name varchar(100) NOT NULL,
    CONSTRAINT fk_position_permissions_position FOREIGN KEY (position_id)
        REFERENCES sys_positions (id) ON DELETE CASCADE,
    CONSTRAINT fk_position_permissions_permission FOREIGN KEY (permission_id)
        REFERENCES sys_permissions (id) ON DELETE RESTRICT,
    CONSTRAINT uk_position_permissions_permission UNIQUE (position_id, permission_id)
);

CREATE INDEX ix_positions_sort ON sys_positions (sort_order, id);
CREATE INDEX ix_position_permissions_permission ON sys_position_permissions (permission_id);

COMMENT ON TABLE sys_positions IS '岗位聚合，维护岗位及权限集合，不直接改变现有用户鉴权';
COMMENT ON TABLE sys_position_permissions IS '岗位权限子实体，系统及权限信息为配置时快照';
COMMENT ON COLUMN sys_positions.id IS '岗位主键，后端生成的26位ULID';
COMMENT ON COLUMN sys_positions.code IS '岗位编码，全局唯一且区分大小写，创建后不可修改';
COMMENT ON COLUMN sys_positions.name IS '岗位名称';
COMMENT ON COLUMN sys_positions.description IS '岗位职责说明，可空';
COMMENT ON COLUMN sys_positions.enabled IS '是否启用，默认true，仅保存岗位状态';
COMMENT ON COLUMN sys_positions.sort_order IS '展示顺序，非负整数，默认0，越小越靠前';
COMMENT ON COLUMN sys_positions.created_at IS '创建时间，北京时间Asia/Shanghai，不含时区偏移';
COMMENT ON COLUMN sys_positions.updated_at IS '最近修改时间，北京时间Asia/Shanghai，权限集合变化也更新';
COMMENT ON COLUMN sys_positions.version IS '聚合乐观锁版本，默认0，保护岗位及权限子实体并发修改';
COMMENT ON COLUMN sys_position_permissions.id IS '岗位权限子实体主键，26位ULID';
COMMENT ON COLUMN sys_position_permissions.position_id IS '所属岗位ID，外键关联sys_positions，岗位删除时级联删除';
COMMENT ON COLUMN sys_position_permissions.permission_id IS '权限定义ID，外键关联sys_permissions，有引用时禁止删除权限定义';
COMMENT ON COLUMN sys_position_permissions.system_code IS '配置时的所属系统编码快照';
COMMENT ON COLUMN sys_position_permissions.system_name IS '配置时的所属系统名称快照，重新提交权限集合时刷新';
COMMENT ON COLUMN sys_position_permissions.permission_code IS '配置时的权限编码快照';
COMMENT ON COLUMN sys_position_permissions.permission_name IS '配置时的权限名称快照，重新提交权限集合时刷新';
