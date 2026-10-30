package com.dedsi.arche.system.domain.systems;

import java.util.Optional;

/** 系统聚合仓储契约；加锁方法须在事务内调用。 */
public interface IManagedSystemRepository {
    /** 按标识读取，不存在时返回空结果。 */
    Optional<ManagedSystem> findById(String id);

    /** 编辑和删除时锁定系统，协调权限写入与系统更名。 */
    Optional<ManagedSystem> lockById(String id);

    /** 权限写入时按不可变编码锁定系统。 */
    Optional<ManagedSystem> lockByCode(String code);

    /** 检查编码是否已存在；数据库唯一键负责并发兜底。 */
    boolean existsByCode(String code);

    /** 保存系统聚合。 */
    ManagedSystem save(ManagedSystem system);

    /** 删除系统聚合。 */
    void delete(ManagedSystem system);
}
