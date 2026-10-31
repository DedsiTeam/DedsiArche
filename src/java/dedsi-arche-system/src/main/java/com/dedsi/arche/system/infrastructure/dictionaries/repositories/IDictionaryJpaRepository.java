package com.dedsi.arche.system.infrastructure.dictionaries.repositories;

import com.dedsi.arche.system.domain.dictionaries.Dictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/** 字典 Spring Data 仓储，ULID 主键；完整读取使用单 SQL 防止跨快照拼接。 */
public interface IDictionaryJpaRepository extends JpaRepository<Dictionary, String> {
    /** 左连接保留没有子项的字典。 */
    @Override
    @Query("select distinct d from Dictionary d left join fetch d.items where d.id = :id")
    Optional<Dictionary> findById(@Param("id") String id);

    /** 不在 fetch 条件中过滤子项，保证取得完整聚合。 */
    @Query("""
            select distinct d from Dictionary d left join fetch d.items
            where d.systemCode = :systemCode and d.code = :code
            """)
    Optional<Dictionary> findBySystemCodeAndCode(
            @Param("systemCode") String systemCode, @Param("code") String code
    );

    /** 按根 ID 稳定加载，用于系统名称同步的固定写入顺序。 */
    @Query("""
            select distinct d from Dictionary d left join fetch d.items
            where d.systemCode = :systemCode order by d.id
            """)
    List<Dictionary> findAllBySystemCode(@Param("systemCode") String systemCode);

    /** 检查系统下字典编码唯一性。 */
    boolean existsBySystemCodeAndCode(String systemCode, String code);

    /** 检查系统是否被字典引用。 */
    boolean existsBySystemCode(String systemCode);
}
