package com.dedsi.arche.system.domain.loginaudits;

/** 登录事实仅允许新增，查询通过独立只读契约。 */
public interface ILoginAuditRepository {
    /** 加入调用方事务并立即暴露写入错误。 */
    void append(LoginAudit audit);
}
