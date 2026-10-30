package com.dedsi.arche.system.api.users;

import com.dedsi.arche.common.exception.BusinessException;

import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/**
 * 统一使用 ProblemDetail，避免数据库细节泄露给调用者。
 */
@RestControllerAdvice
public class UserExceptionHandler {

    /**
     * 将业务错误转换为参数错误或账号冲突响应，并保留错误码。
     */
    @ExceptionHandler(BusinessException.class)
    ProblemDetail business(BusinessException e) {
        var result =
                ProblemDetail.forStatusAndDetail(
                        e.getCode().equals("USERNAME_EXISTS")
                                ? HttpStatus.CONFLICT
                                : HttpStatus.BAD_REQUEST,
                        e.getMessage());
        result.setProperty("code", e.getCode());
        return result;
    }

    /**
     * 登录失败只提示账号密码错误，不误导调用者提供访问令牌。
     */
    @ExceptionHandler(LoginFailedException.class)
    ProblemDetail login(LoginFailedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "账号或密码错误");
    }

    /**
     * 返回统一认证失败响应，避免泄露账号或令牌细节。
     */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail auth(AuthenticationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "认证失败或令牌无效");
    }

    /**
     * 将请求参数校验失败转换为 HTTP 400 响应。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "请求参数不符合约束");
    }

    /**
     * 将数据库完整性冲突转换为 HTTP 409 响应，隐藏数据库细节。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "数据冲突，请检查账号等唯一字段");
    }

    /**
     * 将乐观锁冲突转换为 HTTP 409 响应，提示重新读取数据。
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail version(OptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "数据已变化，请重新读取后提交");
    }
}
