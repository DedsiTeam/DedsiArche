package com.dedsi.arche.system.domain.organizations;

import java.util.List;
import java.util.Optional;

/** 独立组织节点聚合仓储；树关系由父标识连接，不级联保存子树。 */
public interface IOrganizationRepository {
    /** 按标识读取节点。 */
    Optional<Organization> findById(String id);
    /** 固定 ID 顺序读取系统节点，供同步事件使用。 */
    List<Organization> findAllBySystemCode(String systemCode);
    /** 检查系统内编码重复。 */
    boolean existsBySystemCodeAndCode(String systemCode, String code);
    /** 检查系统引用。 */
    boolean existsBySystemCode(String systemCode);
    /** 检查全部直属子节点，包括停用节点。 */
    boolean existsByParentId(String parentId);
    /** 检查启用直属子节点。 */
    boolean existsByParentIdAndEnabledTrue(String parentId);
    /** 保存独立节点。 */
    Organization save(Organization organization);
    /** 删除已确认没有子节点的组织。 */
    void delete(Organization organization);
    /** 提交 SQL 约束与根版本检查。 */
    void flush();
}
