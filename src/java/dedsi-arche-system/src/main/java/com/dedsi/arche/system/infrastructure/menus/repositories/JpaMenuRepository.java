package com.dedsi.arche.system.infrastructure.menus.repositories;

import com.dedsi.arche.system.domain.menus.Menu;
import com.dedsi.arche.system.domain.menus.IMenuRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/** 菜单聚合仓储适配器。 */
@Repository
public class JpaMenuRepository implements IMenuRepository {
    /* Spring Data 持久化委托。 */
    private final IMenuJpaRepository delegate;

    /** 注入持久化接口。 */
    public JpaMenuRepository(IMenuJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Menu> findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public List<Menu> findAllBySystemCode(String systemCode) {
        return delegate.findAllBySystemCodeOrderById(systemCode);
    }

    @Override
    public boolean existsBySystemCodeAndCode(String systemCode, String code) {
        return delegate.existsBySystemCodeAndCode(systemCode, code);
    }

    @Override
    public boolean existsBySystemCode(String systemCode) {
        return delegate.existsBySystemCode(systemCode);
    }

    @Override
    public boolean existsByParentId(String parentId) {
        return delegate.existsByParentId(parentId);
    }

    @Override
    public boolean existsByParentIdAndEnabledTrue(String parentId) {
        return delegate.existsByParentIdAndEnabledTrue(parentId);
    }

    @Override
    public boolean existsPageRoute(String systemCode, String routePath, String excludedId) {
        return delegate.existsPageRoute(systemCode, routePath, excludedId);
    }

    @Override
    public boolean existsByPermissionId(String permissionId) {
        return delegate.existsByPermissionId(permissionId);
    }

    @Override
    public List<Menu> findAllByPermissionId(String permissionId) {
        return delegate.findAllByPermissionIdOrderById(permissionId);
    }

    @Override
    public Menu save(Menu organization) {
        return delegate.save(organization);
    }

    @Override
    public void delete(Menu organization) {
        delegate.delete(organization);
    }

    @Override
    public void flush() {
        delegate.flush();
    }
}
