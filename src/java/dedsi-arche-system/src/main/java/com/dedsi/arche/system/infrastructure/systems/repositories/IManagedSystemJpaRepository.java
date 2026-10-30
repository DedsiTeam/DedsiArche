package com.dedsi.arche.system.infrastructure.systems.repositories;

import com.dedsi.arche.system.domain.systems.ManagedSystem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** 系统的 Spring Data 持久化接口，使用 ULID 字符串主键。 */
public interface IManagedSystemJpaRepository extends JpaRepository<ManagedSystem, String> {
    /** 按不可变编码检查唯一性。 */
    boolean existsByCode(String code);

    /** 系统更新和删除共享同一行锁。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ManagedSystem s where s.id = :id")
    Optional<ManagedSystem> lockById(@Param("id") String id);

    /** 权限写入与系统更名、删除串行化，避免读到过期系统名称。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ManagedSystem s where s.code = :code")
    Optional<ManagedSystem> lockByCode(@Param("code") String code);
}
