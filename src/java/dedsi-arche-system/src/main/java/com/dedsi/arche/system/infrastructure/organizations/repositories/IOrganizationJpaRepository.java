package com.dedsi.arche.system.infrastructure.organizations.repositories;

import com.dedsi.arche.system.domain.organizations.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Spring Data 组织节点持久化，不将父子关系映射为级联实体集合。 */
public interface IOrganizationJpaRepository extends JpaRepository<Organization, String> {
    /** 系统同步固定写入顺序。 */
    List<Organization> findAllBySystemCodeOrderById(String systemCode);
    /** 系统内编码唯一检查。 */
    boolean existsBySystemCodeAndCode(String systemCode, String code);
    /** 系统删除引用检查。 */
    boolean existsBySystemCode(String systemCode);
    /** 叶子删除检查。 */
    boolean existsByParentId(String parentId);
    /** 停用前检查启用子节点。 */
    boolean existsByParentIdAndEnabledTrue(String parentId);
}
