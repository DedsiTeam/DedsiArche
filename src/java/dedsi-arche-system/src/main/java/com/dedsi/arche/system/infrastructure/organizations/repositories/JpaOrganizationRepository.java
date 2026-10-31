package com.dedsi.arche.system.infrastructure.organizations.repositories;

import com.dedsi.arche.system.domain.organizations.Organization;
import com.dedsi.arche.system.domain.organizations.IOrganizationRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/** 组织聚合仓储适配器。 */
@Repository
public class JpaOrganizationRepository implements IOrganizationRepository {
    /* Spring Data 持久化委托。 */
    private final IOrganizationJpaRepository delegate;

    /** 注入持久化接口。 */
    public JpaOrganizationRepository(IOrganizationJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Organization> findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public List<Organization> findAllBySystemCode(String systemCode) {
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
    public Organization save(Organization organization) {
        return delegate.save(organization);
    }

    @Override
    public void delete(Organization organization) {
        delegate.delete(organization);
    }

    @Override
    public void flush() {
        delegate.flush();
    }
}
