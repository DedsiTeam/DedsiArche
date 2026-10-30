package com.dedsi.arche.system.domain.systems.query;

import com.dedsi.arche.system.domain.systems.SystemStatus;
import java.util.List;

/** 系统只读查询契约。 */
public interface IManagedSystemQuery {
    /** 查询全部系统下拉选项，仅返回编码和名称。 */
    List<ManagedSystemOptionDto> options();

    /** 按编码或名称搜索，并可按状态筛选。 */
    ManagedSystemPage search(String keyword, SystemStatus status, int page, int size);
}
