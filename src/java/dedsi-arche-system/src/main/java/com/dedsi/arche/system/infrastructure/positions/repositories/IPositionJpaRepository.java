package com.dedsi.arche.system.infrastructure.positions.repositories;

import com.dedsi.arche.system.domain.positions.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

/** 岗位 Spring Data 仓储，主键为 ULID 字符串。 */
public interface IPositionJpaRepository extends JpaRepository<Position, String> {
    /**
     * 单条查询加载岗位及全部权限子实体，避免并发修改时读取到不同版本的聚合片段。
     * 左连接保留无权限的岗位，不存在时仍返回空结果；详情、修改及删除共用此加载入口。
     */
    @Override
    @Query("select distinct p from Position p left join fetch p.permissions where p.id = :id")
    Optional<Position> findById(@Param("id") String id);

    /** 使用独立 EXISTS 条件筛选岗位，不能过滤 fetch 集合，否则会丢失其他权限子实体。 */
    @Query("""
            select distinct p from Position p left join fetch p.permissions
            where exists (
                select item.id from PositionPermission item
                where item.position = p and item.permissionId = :permissionId
            )
            order by p.id
            """)
    List<Position> findAllByPermissionId(@Param("permissionId") String permissionId);

    /** 根及子项系统引用使用 EXISTS 筛选，fetch 始终加载全部权限集合。 */
    @Query("""
            select distinct p from Position p left join fetch p.permissions
            where p.systemCode = :systemCode or exists (
                select item.id from PositionPermission item
                where item.position = p and item.systemCode = :systemCode
            )
            order by p.id
            """)
    List<Position> findAllReferencingSystem(@Param("systemCode") String systemCode);

    /** 检查岗位根的系统引用。 */
    boolean existsBySystemCode(String systemCode);

    /** 检查岗位编码唯一性；最终由数据库约束兜底。 */
    boolean existsByCode(String code);
}
