package com.dedsi.arche.system.api.loginaudits.request;

import com.dedsi.arche.system.domain.loginaudits.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/** 登录审计筛选，时间使用北京时间半开区间。 */
public record LoginAuditPageRequest(
        /* 同时匹配账号或姓名。 */
        @Size(max = 200) String keyword,
        /* 账号包含匹配，忽略大小写。 */
        @Size(max = 200) String loginAccount,
        /* 姓名包含匹配，忽略大小写。 */
        @Size(max = 100) String displayName,
        /* 连接 IP 精确匹配。 */
        @Size(max = 64) String clientIp,
        /* 可选结果条件。 */
        LoginAuditResult result,
        /* 可选原因条件。 */
        LoginAuditReason reasonCode,
        /* 包含的起始时间，可空。 */
        @Schema(description = "北京时间起始时间（包含），ISO 本地时间，无时区后缀")
        LocalDateTime from,
        /* 不包含的截止时间，可空。 */
        @Schema(description = "北京时间截止时间（不包含），ISO 本地时间，无时区后缀")
        LocalDateTime to,
        /* 默认第 1 页。 */
        @Min(1) @Max(1000000) Integer page,
        /* 默认 10 条，最大 100。 */
        @Min(1) @Max(100) Integer size
) {
    /** 规范化文本条件及分页默认值。 */
    public LoginAuditPageRequest {
        keyword = trim(keyword);
        loginAccount = trim(loginAccount);
        displayName = trim(displayName);
        clientIp = trim(clientIp);
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
