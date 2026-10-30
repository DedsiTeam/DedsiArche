-- 岗位领域完整结构快照，截至 Flyway V11；仅供阅读，不执行到现有数据库。

CREATE TABLE sys_positions (
    id varchar(26) PRIMARY KEY,
    system_code varchar(100),
    system_name varchar(100),
    code varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(500),
    enabled boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_positions_code UNIQUE (code),
    CONSTRAINT ck_positions_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_positions_system_pair CHECK (
        (system_code IS NULL AND system_name IS NULL)
        OR (system_code IS NOT NULL AND system_name IS NOT NULL)
    ),
    CONSTRAINT fk_positions_system FOREIGN KEY (system_code)
        REFERENCES sys_systems (code) ON DELETE RESTRICT
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

CREATE INDEX ix_positions_system_code ON sys_positions (system_code);
CREATE INDEX ix_position_permissions_system ON sys_position_permissions (system_code, position_id);
CREATE INDEX ix_positions_sort ON sys_positions (sort_order, id);
CREATE INDEX ix_position_permissions_permission ON sys_position_permissions (permission_id);

COMMENT ON TABLE sys_positions IS '岗位聚合，维护岗位及权限集合，不直接改变现有用户鉴权';
COMMENT ON TABLE sys_position_permissions IS '岗位权限子实体，系统及权限名称由相应领域事件同事务同步';
COMMENT ON COLUMN sys_positions.id IS '岗位主键，后端生成的26位ULID';
COMMENT ON COLUMN sys_positions.code IS '岗位编码，全局唯一且区分大小写，创建后不可修改';
COMMENT ON COLUMN sys_positions.name IS '岗位名称';
COMMENT ON COLUMN sys_positions.description IS '岗位职责说明，可空';
COMMENT ON COLUMN sys_positions.enabled IS '是否启用，默认true，仅保存岗位状态';
COMMENT ON COLUMN sys_positions.sort_order IS '展示顺序，非负整数，默认0，越小越靠前';
COMMENT ON COLUMN sys_positions.created_at IS '创建时间，北京时间Asia/Shanghai，不含时区偏移';
COMMENT ON COLUMN sys_positions.updated_at IS '最近修改时间，北京时间Asia/Shanghai；岗位配置或引用的系统及权限名称实际变化时更新';
COMMENT ON COLUMN sys_positions.version IS '聚合乐观锁版本，默认0；岗位配置、系统名称及权限名称同步均参与并发保护';
COMMENT ON COLUMN sys_position_permissions.id IS '岗位权限子实体主键，26位ULID';
COMMENT ON COLUMN sys_position_permissions.position_id IS '所属岗位ID，外键关联sys_positions，岗位删除时级联删除';
COMMENT ON COLUMN sys_position_permissions.permission_id IS '权限定义ID，外键关联sys_permissions，有引用时禁止删除权限定义';
COMMENT ON COLUMN sys_position_permissions.system_code IS '配置时的所属系统编码快照';
COMMENT ON COLUMN sys_position_permissions.system_name IS '所属系统当前名称；系统改名或首次登记时通过同步领域事件更新，配置权限时也刷新';
COMMENT ON COLUMN sys_position_permissions.permission_code IS '配置时的权限编码快照';
COMMENT ON COLUMN sys_position_permissions.permission_name IS '权限当前名称；权限改名时通过同步领域事件更新，重新配置权限时也刷新';
COMMENT ON COLUMN sys_positions.system_code IS '岗位所属系统编码，外键关联sys_systems(code)；历史未登记为空，首次登记后不可修改';
COMMENT ON COLUMN sys_positions.system_name IS '所属系统当前名称，由服务端读取并通过系统改名事件同事务同步；历史未登记为空，与system_code同时空或非空';
