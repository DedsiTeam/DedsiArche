-- 权限名称改为同事务同步的当前名称；系统名称仍保留原有快照语义。
COMMENT ON TABLE sys_position_permissions IS '岗位权限子实体，系统信息为配置时快照，权限名称由改名领域事件同事务同步';
COMMENT ON COLUMN sys_position_permissions.permission_name IS '权限当前名称；权限改名时通过同步领域事件更新，重新配置权限时也刷新';
COMMENT ON COLUMN sys_positions.updated_at IS '最近修改时间，北京时间Asia/Shanghai；岗位配置或引用权限名称实际变化时更新';
COMMENT ON COLUMN sys_positions.version IS '聚合乐观锁版本，默认0；岗位配置及引用权限名称同步均参与并发保护';
