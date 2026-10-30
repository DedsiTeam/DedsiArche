package com.dedsi.arche.system.domain.permissions.query;

/**
 * 权限只读查询契约，与聚合写入仓储分离。
 */
public interface IPermissionQuery {

    /**
     * 按关键词、系统和状态查询，使用固定排序保证分页顺序稳定。
     */
    PermissionPage search(String keyword, String systemCode, Boolean enabled, int page, int size);
}
