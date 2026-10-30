package com.dedsi.arche.system.infrastructure.permissions.repositories;

import com.dedsi.arche.system.domain.permissions.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 权限实体的 Spring Data 数据访问接口，主键类型为 ULID 字符串。
 */
public interface IPermissionJpaRepository extends JpaRepository<Permission, String> {

    /**
     * 检查同一系统下的权限编码是否已存在。
     */
    boolean existsBySystemCodeAndCode(String systemCode, String code);
}
