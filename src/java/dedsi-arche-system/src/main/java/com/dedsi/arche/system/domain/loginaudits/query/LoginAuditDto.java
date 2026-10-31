package com.dedsi.arche.system.domain.loginaudits.query;

import com.dedsi.arche.system.domain.loginaudits.*;
import java.time.LocalDateTime;
import java.util.UUID;

/** 列表与详情共享的只读审计投影，不包含密码或令牌。 */
public record LoginAuditDto(
        /* 审计 ULID 标识。 */
        String id,
        /* 北京时间登录尝试时间。 */
        LocalDateTime loginTime,
        /* 登录结果。 */
        LoginAuditResult result,
        /* 受控原因编码。 */
        LoginAuditReason reasonCode,
        /* 登录账号历史快照，可空。 */
        String loginAccount,
        /* 已确认身份的用户逻辑标识，无外键级联。 */
        UUID userId,
        /* 用户显示名称历史快照，可空。 */
        String displayName,
        /* 连接来源 IP，不信任转发头。 */
        String clientIp,
        /* 截断清洗后的客户端标识。 */
        String userAgent,
        /* 由原因枚举产生的受控失败文案。 */
        String failureMessage,
        /* 服务端关联标识，不采信客户端头。 */
        String traceId
) {
    /** 从不可变事实构造响应。 */
    public static LoginAuditDto from(LoginAudit value) {
        return new LoginAuditDto(
                value.getId(), value.getLoginTime(), value.getResult(),
                value.getReasonCode(), value.getLoginAccount(), value.getUserId(),
                value.getDisplayName(), value.getClientIp(), value.getUserAgent(),
                value.getFailureMessage(), value.getTraceId()
        );
    }
}
