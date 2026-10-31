package com.dedsi.arche.system.infrastructure.menus.repositories;

import com.dedsi.arche.system.domain.menus.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data 菜单节点持久化，不将父子关系映射为级联实体集合。 */
public interface IMenuJpaRepository extends JpaRepository<Menu, String> {
    /** 只约束站内页面路由，忽略自身。 */
    @Query("""
            select count(m) > 0 from Menu m where m.systemCode = :systemCode
            and m.menuType = com.dedsi.arche.system.domain.menus.MenuType.PAGE
            and m.routePath = :routePath and (:excludedId is null or m.id <> :excludedId)
            """)
    boolean existsPageRoute(
            @Param("systemCode") String systemCode, @Param("routePath") String routePath,
            @Param("excludedId") String excludedId
    );
    /** 权限删除保护。 */
    boolean existsByPermissionId(String permissionId);
    /** 权限名称同步固定顺序。 */
    List<Menu> findAllByPermissionIdOrderById(String permissionId);
    /** 系统同步固定写入顺序。 */
    List<Menu> findAllBySystemCodeOrderById(String systemCode);
    /** 系统内编码唯一检查。 */
    boolean existsBySystemCodeAndCode(String systemCode, String code);
    /** 系统删除引用检查。 */
    boolean existsBySystemCode(String systemCode);
    /** 叶子删除检查。 */
    boolean existsByParentId(String parentId);
    /** 停用前检查启用子节点。 */
    boolean existsByParentIdAndEnabledTrue(String parentId);
}
