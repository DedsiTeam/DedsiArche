package com.dedsi.arche.system.domain.loginaudits.query;

import com.dedsi.arche.system.domain.loginaudits.*;
import java.time.LocalDateTime;
import java.util.Optional;

/** 登录审计只读查询契约。 */
public interface ILoginAuditQuery {
    /** 时间范围为北京时间半开区间，结果与原因条件独立组合。 */
    LoginAuditPage search(
            String keyword, String loginAccount, String displayName, String clientIp,
            LoginAuditResult result, LoginAuditReason reasonCode, LocalDateTime from, LocalDateTime to,
            int page, int size
    );
    /** 不存在返回空。 */
    Optional<LoginAuditDto> findById(String id);
}
