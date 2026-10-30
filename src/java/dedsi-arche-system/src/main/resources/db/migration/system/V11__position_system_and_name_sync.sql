ALTER TABLE sys_positions
    ADD COLUMN system_code varchar(100),
    ADD COLUMN system_name varchar(100),
    ADD CONSTRAINT ck_positions_system_pair CHECK (
        (system_code IS NULL AND system_name IS NULL)
        OR (system_code IS NOT NULL AND system_name IS NOT NULL)
    ),
    ADD CONSTRAINT fk_positions_system FOREIGN KEY (system_code)
        REFERENCES sys_systems (code) ON DELETE RESTRICT;

CREATE INDEX ix_positions_system_code ON sys_positions (system_code);
CREATE INDEX ix_position_permissions_system ON sys_position_permissions (system_code, position_id);

COMMENT ON COLUMN sys_positions.system_code IS '岗位所属系统编码，外键关联sys_systems(code)；历史未登记为空，首次登记后不可修改';
COMMENT ON COLUMN sys_positions.system_name IS '所属系统当前名称，由服务端读取并通过系统改名事件同事务同步；历史未登记为空，与system_code同时空或非空';
COMMENT ON TABLE sys_position_permissions IS '岗位权限子实体，系统及权限名称由相应领域事件同事务同步';
COMMENT ON COLUMN sys_position_permissions.system_name IS '所属系统当前名称；系统改名或首次登记时通过同步领域事件更新，配置权限时也刷新';
COMMENT ON COLUMN sys_positions.updated_at IS '最近修改时间，北京时间Asia/Shanghai；岗位配置或引用的系统及权限名称实际变化时更新';
COMMENT ON COLUMN sys_positions.version IS '聚合乐观锁版本，默认0；岗位配置、系统名称及权限名称同步均参与并发保护';
