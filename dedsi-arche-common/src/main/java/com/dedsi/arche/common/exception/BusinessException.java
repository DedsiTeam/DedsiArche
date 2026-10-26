package com.dedsi.arche.common.exception;
/** 可向调用方公开的业务异常，不包含数据库或认证内部信息。 */
public class BusinessException extends RuntimeException {
    private final String code;
    public BusinessException(String code, String message) { super(message); this.code = code; }
    public String getCode() { return code; }
}
