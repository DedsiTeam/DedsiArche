package com.dedsi.arche.system.domain.organizations.query;

import java.util.List;

/** 组织只读查询与写聚合仓储分离。 */
public interface IOrganizationQuery {
    /** 分页筛选节点，不将过滤结果拼为残缺树。 */
    OrganizationPage search(String keyword, String systemCode, Boolean enabled, int page, int size);
    /** 单次查询加载全部节点，构造系统完整树。 */
    List<OrganizationTreeDto> tree(String systemCode);
}
