package com.dedsi.arche.system.domain.menus.query;

import java.util.List;
import com.dedsi.arche.system.domain.menus.MenuType;

/** 菜单只读查询与写聚合仓储分离。 */
public interface IMenuQuery {
    /** 分页筛选节点，不将过滤结果拼为残缺树。 */
    MenuPage search(String keyword, String systemCode, Boolean enabled, MenuType menuType, int page, int size);
    /** 单次查询加载全部节点，构造系统完整树。 */
    List<MenuTreeDto> tree(String systemCode);
}
