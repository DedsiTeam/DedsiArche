-- 用户领域（含用户岗位及认证内部刷新会话）：截至 Flyway V12 的完整结构快照。
-- 用于阅读和结构参考，不属于 Flyway 迁移，不由应用自动执行。
-- 不要在已有业务库执行；新环境仍通过完整 Flyway 迁移链初始化。

CREATE TABLE sys_users (
    id uuid PRIMARY KEY,
    username varchar(50) NOT NULL,
    display_name varchar(100) NOT NULL,
    password_hash varchar(100) NOT NULL,
    enabled boolean NOT NULL,
    created_at timestamp without time zone NOT NULL,
    version bigint NOT NULL,
    credential_version bigint NOT NULL DEFAULT 0,
    positions_revision bigint NOT NULL DEFAULT 0,
    phone varchar(16),
    email varchar(254),
    CONSTRAINT uk_users_username UNIQUE (username)
);
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

CREATE TABLE sys_refresh_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES sys_users(id),
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamp without time zone NOT NULL,
    revoked boolean NOT NULL,
    credential_version bigint NOT NULL DEFAULT 0
);
CREATE INDEX ix_refresh_sessions_user ON sys_refresh_sessions(user_id);
CREATE INDEX ix_refresh_sessions_expiry ON sys_refresh_sessions(expires_at);

COMMENT ON TABLE sys_users IS '用户聚合，维护账户资料、用户岗位快照集合及认证凭据版本';
COMMENT ON COLUMN sys_users.id IS '用户唯一标识，UUID';
COMMENT ON COLUMN sys_users.username IS '登录账号，去除首尾空白并规范化为小写，全局唯一';
COMMENT ON COLUMN sys_users.display_name IS '用户姓名，用于界面展示';
COMMENT ON COLUMN sys_users.password_hash IS 'BCrypt 密码哈希，不存储明文密码';
COMMENT ON COLUMN sys_users.enabled IS '账户是否启用；停用后禁止登录和访问受保护接口';
COMMENT ON COLUMN sys_users.created_at IS '账户创建时间，北京时间（Asia/Shanghai）本地值';
COMMENT ON COLUMN sys_users.version IS '用户聚合JPA乐观锁版本，保护账户资料与岗位集合的并发修改';
COMMENT ON COLUMN sys_users.credential_version IS '凭据版本，重置密码时递增，使旧访问令牌和刷新凭据失效';
COMMENT ON COLUMN sys_users.phone IS '手机号，可为空；7 至 15 位数字，可带国际区号前缀 +，不设唯一约束';
COMMENT ON COLUMN sys_users.email IS '邮箱，可为空，最长 254 字符，不设唯一约束';

COMMENT ON TABLE sys_refresh_sessions IS '认证内部刷新会话，保存令牌摘要、有效期和撤销状态';
COMMENT ON COLUMN sys_refresh_sessions.id IS '刷新会话唯一标识，UUID；认证内部记录，非独立业务领域';
COMMENT ON COLUMN sys_refresh_sessions.user_id IS '所属用户 UUID，关联 sys_users.id';
COMMENT ON COLUMN sys_refresh_sessions.token_hash IS '刷新令牌的 SHA-256 十六进制摘要，唯一，不保存令牌明文';
COMMENT ON COLUMN sys_refresh_sessions.expires_at IS '刷新会话到期时间，北京时间（Asia/Shanghai）本地值';
COMMENT ON COLUMN sys_refresh_sessions.revoked IS '是否已撤销；刷新轮换、退出或撤销会话后不可再使用';
COMMENT ON COLUMN sys_refresh_sessions.credential_version IS '签发时的用户凭据版本；与用户当前版本不一致时会话失效';
COMMENT ON COLUMN sys_users.positions_revision IS '内部岗位修订计数，默认0；岗位集合或快照实际变化时递增以触发用户根乐观锁，不用于认证';
COMMENT ON TABLE sys_user_positions IS '用户岗位关联子实体，保存配置时岗位及所属系统快照，不参与实际授权判定';
COMMENT ON COLUMN sys_user_positions.id IS '关联主键，后端生成的26位ULID';
COMMENT ON COLUMN sys_user_positions.user_id IS '所属用户UUID，外键关联sys_users.id；删除用户时级联删除关联';
COMMENT ON COLUMN sys_user_positions.position_id IS '岗位ULID，外键关联sys_positions.id；被引用岗位禁止删除';
COMMENT ON COLUMN sys_user_positions.position_code IS '配置时的岗位编码快照，重新提交岗位集合时由服务端刷新';
COMMENT ON COLUMN sys_user_positions.position_name IS '配置时的岗位名称快照，岗位单独改名不自动同步';
COMMENT ON COLUMN sys_user_positions.system_code IS '配置时岗位所属系统编码快照，旧岗位未登记系统时为空，与system_name成对为空或非空，不是系统外键';
COMMENT ON COLUMN sys_user_positions.system_name IS '配置时岗位所属系统名称快照，系统单独改名不自动同步；重新配置岗位集合时刷新';
