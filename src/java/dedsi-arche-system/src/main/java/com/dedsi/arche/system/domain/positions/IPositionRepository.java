package com.dedsi.arche.system.domain.positions;

import java.util.Optional;
import java.util.List;

/** 岗位聚合持久化契约。 */
public interface IPositionRepository {
    /** 按 ULID 读取岗位，不存在时返回空。 */
    Optional<Position> findById(String id);

    /** 用户分配岗位前读取并锁定岗位根，不加载权限集合；调用方按 ID 顺序读取。 */
    Optional<Position> lockByIdForUser(String id);

    /** 删除前锁定岗位根，协调用户分配与引用检查。 */
    Optional<Position> lockByIdForDeletion(String id);

    /** 按岗位 ID 排序，完整加载引用指定权限的岗位及全部子实体。 */
    List<Position> findAllByPermissionId(String permissionId);

    /** 完整加载根或任一子项引用指定系统的岗位，按岗位 ID 排序。 */
    List<Position> findAllReferencingSystem(String systemCode);

    /** 检查是否有岗位根登记在指定系统下，用于系统删除保护。 */
    boolean existsBySystemCode(String systemCode);

    /** 检查规范化后的岗位编码是否已存在。 */
    boolean existsByCode(String code);

    /** 保存岗位及权限子实体。 */
    Position save(Position position);

    /** 删除岗位及其子实体。 */
    void delete(Position position);

    /** 同步约束与版本，使响应包含持久化后的版本。 */
    void flush();
}
