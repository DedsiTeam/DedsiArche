package com.dedsi.arche.system.domain.positions.query;

/** 岗位只读投影查询契约。 */
public interface IPositionQuery {
    /** 按编码或名称搜索，并按启用状态筛选。 */
    PositionPage search(String keyword, Boolean enabled, int page, int size);
}
