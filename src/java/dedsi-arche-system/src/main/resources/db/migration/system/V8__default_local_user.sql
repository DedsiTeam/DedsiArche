-- 新环境默认用户；管理员权限仍由部署配置中的管理员名单决定。
-- 密码使用现有 BCrypt 工作因子 12，仅保存哈希；同名账号存在时保持原密码和资料。
INSERT INTO sys_users (
    id,
    username,
    display_name,
    password_hash,
    enabled,
    created_at,
    version,
    credential_version
)
VALUES (
    gen_random_uuid(),
    'dedsi_user',
    '本地管理员',
    '$2a$12$BLOc8UAqcqaXEtpUr1zpeew7LQM5Q4Q4JzeFZuBcvASCV.6C7AG6S',
    true,
    CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Shanghai',
    0,
    0
)
ON CONFLICT (username) DO NOTHING;
