package com.dedsi.arche.system.domain.menus;

import java.util.List;
import java.util.Optional;

/** 独立菜单节点聚合仓储；树关系由父标识连接，不级联保存子树。 */
public interface IMenuRepository {
    /** 按标识读取节点。 */
    Optional<Menu> findById(String id);
    /** 固定 ID 顺序读取系统节点，供同步事件使用。 */
    List<Menu> findAllBySystemCode(String systemCode);
    /** 检查系统内编码重复。 */
    boolean existsBySystemCodeAndCode(String systemCode, String code);
    /** 检查系统引用。 */
    boolean existsBySystemCode(String systemCode);
    /** 检查全部直属子节点，包括停用节点。 */
    boolean existsByParentId(String parentId);
    /** 检查启用直属子节点。 */
    boolean existsByParentIdAndEnabledTrue(String parentId);
    /** 检查页面路由在系统内是否被其他节点占用。 */
    boolean existsPageRoute(String systemCode, String routePath, String excludedId);
    /** 权限删除引用保护。 */
    boolean existsByPermissionId(String permissionId);
    /** 按 ID 同步权限名称。 */
    List<Menu> findAllByPermissionId(String permissionId);
    /** 保存独立节点。 */
    Menu save(Menu organization);
    /** 删除已确认没有子节点的菜单。 */
    void delete(Menu organization);
    /** 提交 SQL 约束与根版本检查。 */
    void flush();
}
