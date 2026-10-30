package com.dedsi.arche.common.exception;

/**
 * 可向调用方公开的业务异常，不包含数据库或认证内部信息。
 */
public class BusinessException extends RuntimeException {

    /* 供调用方识别业务错误的稳定错误码。 */
    private final String code;

    /**
     * 使用业务错误码和可公开的错误说明构造异常。
     */
    public BusinessException(
            String code,
            String message
    ) {
        super(message);
        this.code = code;
    }

    /**
     * 返回供调用方识别业务错误的稳定错误码。
     */
    public String getCode() {
        return code;
    }
}
