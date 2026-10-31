package com.dedsi.arche.system.domain.permissions;

import java.util.Optional;

/**
 * 权限聚合的持久化契约。
 */
public interface IPermissionRepository {

    /** 检查系统下是否存在权限，用于系统删除保护。 */
    boolean existsBySystemCode(String systemCode);

    /** 按权限 ID 顺序加写锁并刷新，供系统名称事件同步使用；调用方已持有系统锁。 */
    java.util.List<Permission> lockAllBySystemCode(String systemCode);


    /**
     * 按 ULID 加载权限，不存在时返回空结果。
     */
    Optional<Permission> findById(String id);

    /** 锁定并刷新权限定义供岗位配置使用；调用方按权限 ID 顺序调用，锁持有到事务结束。 */
    Optional<Permission> lockByIdForPosition(String id);

    /** 菜单绑定时读锁并刷新权限；调用方先持有所属系统锁。 */
    Optional<Permission> lockByIdForMenu(String id);

    /**
     * 检查同一系统下的权限编码是否已存在。
     */
    boolean existsBySystemCodeAndCode(String systemCode, String code);

    /**
     * 保存权限聚合。
     */
    Permission save(Permission permission);

    /**
     * 删除权限；接口入口同时要求匹配当前版本。
     */
    void delete(Permission permission);

    /**
     * 立即同步持久化变更，使约束与版本冲突在事务内暴露。
     */
    void flush();
}
