package com.dedsi.arche.system.infrastructure.dictionaries.repositories;

import com.dedsi.arche.system.domain.dictionaries.Dictionary;
import com.dedsi.arche.system.domain.dictionaries.IDictionaryRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/** Spring Data 字典聚合仓储适配器，不单独保存或删除子项。 */
@Repository
public class JpaDictionaryRepository implements IDictionaryRepository {
    /* 实际数据库操作委托。 */
    private final IDictionaryJpaRepository delegate;

    /** 注入 Spring Data 仓储。 */
    public JpaDictionaryRepository(IDictionaryJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Dictionary> findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public Optional<Dictionary> findBySystemCodeAndCode(String systemCode, String code) {
        return delegate.findBySystemCodeAndCode(systemCode, code);
    }

    @Override
    public List<Dictionary> findAllBySystemCode(String systemCode) {
        return delegate.findAllBySystemCode(systemCode);
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
    public Dictionary save(Dictionary dictionary) {
        return delegate.save(dictionary);
    }

    @Override
    public void delete(Dictionary dictionary) {
        delegate.delete(dictionary);
    }

    @Override
    public void flush() {
        delegate.flush();
    }
}
