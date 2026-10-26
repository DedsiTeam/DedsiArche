package com.dedsi.arche.system.api.users;
import com.dedsi.arche.common.exception.BusinessException;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
/** 统一使用 ProblemDetail，避免数据库细节泄露给调用者。 */
@RestControllerAdvice
public class UserExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ProblemDetail business(BusinessException e) {
        var result=ProblemDetail.forStatusAndDetail(e.getCode().equals("USERNAME_EXISTS")?HttpStatus.CONFLICT:HttpStatus.BAD_REQUEST,e.getMessage());
        result.setProperty("code",e.getCode()); return result;
    }
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail auth(AuthenticationException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,"认证失败或令牌无效"); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"请求参数不符合约束"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"数据冲突，请检查用户名等唯一字段"); }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail version(OptimisticLockingFailureException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"数据已变化，请重新读取后提交"); }
}
