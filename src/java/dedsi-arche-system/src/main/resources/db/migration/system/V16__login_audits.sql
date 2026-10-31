-- 登录审计独立不可变事实，无用户外键、版本或可变业务快照。
CREATE TABLE sys_login_audits (
    id varchar(26) PRIMARY KEY,
    login_time timestamp without time zone NOT NULL,
    result varchar(10) NOT NULL,
    reason_code varchar(30) NOT NULL,
    login_account varchar(200),
    user_id uuid,
    display_name varchar(100),
    client_ip varchar(64),
    user_agent varchar(512),
    failure_message varchar(200),
    trace_id varchar(64),
    CONSTRAINT ck_login_audits_result_reason CHECK (
        (result = 'SUCCESS' AND reason_code = 'LOGIN_SUCCEEDED' AND user_id IS NOT NULL AND failure_message IS NULL)
        OR (result = 'FAILURE' AND reason_code IN ('BAD_CREDENTIALS','USER_DISABLED','INVALID_REQUEST','SERVICE_ERROR')
            AND failure_message IS NOT NULL)
    )
);
CREATE INDEX idx_login_audits_time ON sys_login_audits(login_time DESC, id DESC);
CREATE INDEX idx_login_audits_result_time ON sys_login_audits(result, login_time DESC, id DESC);
CREATE INDEX idx_login_audits_reason_time ON sys_login_audits(reason_code, login_time DESC, id DESC);
CREATE INDEX idx_login_audits_ip_time ON sys_login_audits(client_ip, login_time DESC, id DESC);

COMMENT ON TABLE sys_login_audits IS '登录尝试不可变审计，成功与会话同事务，失败独立事务；不保存密码、令牌或原始请求正文';
COMMENT ON COLUMN sys_login_audits.id IS '服务端生成的登录尝试ULID，同次成功失败补记共用以防重复';
COMMENT ON COLUMN sys_login_audits.login_time IS '登录尝试时间，北京时间Asia/Shanghai';
COMMENT ON COLUMN sys_login_audits.result IS '结果SUCCESS成功或FAILURE失败';
COMMENT ON COLUMN sys_login_audits.reason_code IS '受控原因LOGIN_SUCCEEDED/BAD_CREDENTIALS/USER_DISABLED/INVALID_REQUEST/SERVICE_ERROR';
COMMENT ON COLUMN sys_login_audits.login_account IS '清洗截断后的登录账号快照，无法解析时为空';
COMMENT ON COLUMN sys_login_audits.user_id IS '已确认身份的用户逻辑标识，可空，无外键，删除用户不删除审计';
COMMENT ON COLUMN sys_login_audits.display_name IS '已确认用户显示名称历史快照，可空，不随用户改名变化';
COMMENT ON COLUMN sys_login_audits.client_ip IS 'Servlet连接来源IP，不直接信任转发请求头';
COMMENT ON COLUMN sys_login_audits.user_agent IS '清洗截断的客户端标识，不存完整请求头集合';
COMMENT ON COLUMN sys_login_audits.failure_message IS '由受控原因映射的失败文案，成功为空，不保存原始异常';
COMMENT ON COLUMN sys_login_audits.trace_id IS '服务端生成的关联标识，不信任客户端提供的值';
