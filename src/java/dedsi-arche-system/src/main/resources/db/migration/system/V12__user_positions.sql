ALTER TABLE sys_users ADD COLUMN positions_revision bigint NOT NULL DEFAULT 0;

CREATE TABLE sys_user_positions (
    id varchar(26) PRIMARY KEY,
    user_id uuid NOT NULL,
    position_id varchar(26) NOT NULL,
    position_code varchar(100) NOT NULL,
    position_name varchar(100) NOT NULL,
    system_code varchar(100),
    system_name varchar(100),
    CONSTRAINT uk_user_positions_position UNIQUE (user_id, position_id),
    CONSTRAINT fk_user_positions_user FOREIGN KEY (user_id)
        REFERENCES sys_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_positions_position FOREIGN KEY (position_id)
        REFERENCES sys_positions (id) ON DELETE RESTRICT,
    CONSTRAINT ck_user_positions_system_pair CHECK (
        (system_code IS NULL AND system_name IS NULL)
        OR (system_code IS NOT NULL AND system_name IS NOT NULL)
    )
);

CREATE INDEX ix_user_positions_position ON sys_user_positions (position_id);

COMMENT ON TABLE sys_users IS '用户聚合，维护账户资料、用户岗位快照集合及认证凭据版本';
COMMENT ON COLUMN sys_users.positions_revision IS '内部岗位修订计数，默认0；岗位集合或快照实际变化时递增以触发用户根乐观锁，不用于认证';
COMMENT ON COLUMN sys_users.version IS '用户聚合JPA乐观锁版本，保护账户资料与岗位集合的并发修改';
COMMENT ON TABLE sys_user_positions IS '用户岗位关联子实体，保存配置时岗位及所属系统快照，不参与实际授权判定';
COMMENT ON COLUMN sys_user_positions.id IS '关联主键，后端生成的26位ULID';
COMMENT ON COLUMN sys_user_positions.user_id IS '所属用户UUID，外键关联sys_users.id；删除用户时级联删除关联';
COMMENT ON COLUMN sys_user_positions.position_id IS '岗位ULID，外键关联sys_positions.id；被引用岗位禁止删除';
COMMENT ON COLUMN sys_user_positions.position_code IS '配置时的岗位编码快照，重新提交岗位集合时由服务端刷新';
COMMENT ON COLUMN sys_user_positions.position_name IS '配置时的岗位名称快照，岗位单独改名不自动同步';
COMMENT ON COLUMN sys_user_positions.system_code IS '配置时岗位所属系统编码快照，旧岗位未登记系统时为空，与system_name成对为空或非空，不是系统外键';
COMMENT ON COLUMN sys_user_positions.system_name IS '配置时岗位所属系统名称快照，系统单独改名不自动同步；重新配置岗位集合时刷新';
