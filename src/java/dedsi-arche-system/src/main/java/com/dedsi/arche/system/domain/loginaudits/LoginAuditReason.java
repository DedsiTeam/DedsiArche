package com.dedsi.arche.system.domain.loginaudits;

/** 受控原因，不从异常消息或请求字段拼接。 */
public enum LoginAuditReason {
    /* 登录成功。 */
    LOGIN_SUCCEEDED,
    /* 账号不存在或密码错误，合并记录避免外部账号探测。 */
    BAD_CREDENTIALS,
    /* 密码正确但用户停用。 */
    USER_DISABLED,
    /* 登录 JSON 或参数校验失败。 */
    INVALID_REQUEST,
    /* 登录服务或事务提交失败。 */
    SERVICE_ERROR
}
