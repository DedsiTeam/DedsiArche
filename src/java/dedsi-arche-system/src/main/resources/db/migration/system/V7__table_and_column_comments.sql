-- 补充数据库元数据中的中文说明，不修改字段结构或业务数据。
COMMENT ON TABLE sys_users IS '用户账户，维护登录资料、联系方式、启用状态和凭据版本';
COMMENT ON COLUMN sys_users.id IS '用户唯一标识，UUID';
COMMENT ON COLUMN sys_users.username IS '登录账号，去除首尾空白并规范化为小写，全局唯一';
COMMENT ON COLUMN sys_users.display_name IS '用户姓名，用于界面展示';
COMMENT ON COLUMN sys_users.password_hash IS 'BCrypt 密码哈希，不存储明文密码';
COMMENT ON COLUMN sys_users.enabled IS '账户是否启用；停用后禁止登录和访问受保护接口';
COMMENT ON COLUMN sys_users.created_at IS '账户创建时间，北京时间（Asia/Shanghai）本地值';
COMMENT ON COLUMN sys_users.version IS 'JPA 乐观锁版本，防止并发更新覆盖';
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
