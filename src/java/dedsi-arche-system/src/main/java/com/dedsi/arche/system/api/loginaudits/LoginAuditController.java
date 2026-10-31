package com.dedsi.arche.system.api.loginaudits;

import com.dedsi.arche.system.api.loginaudits.request.LoginAuditPageRequest;
import com.dedsi.arche.system.domain.loginaudits.query.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** 管理员只读审计入口，不提供创建、修改或删除 API。 */
@RestController
@RequestMapping("/api/login-audits")
@Tag(name = "登录审计")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@userAdministration.canManage(authentication)")
public class LoginAuditController {
    private final ILoginAuditQuery query;

    public LoginAuditController(ILoginAuditQuery query) {
        this.query = query;
    }

    /** 时间范围前闭后开，结果与原因互相矛盾时正常返回空结果。 */
    @PostMapping("/page")
    @Operation(summary = "分页查询登录审计", description = "账号/姓名包含匹配，IP 精确匹配；时间为北京时间 [from,to)，按时间与 ID 倒序。")
    public LoginAuditPage page(@Valid @RequestBody LoginAuditPageRequest request) {
        if (request.from() != null && request.to() != null && !request.from().isBefore(request.to())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "起始时间必须早于截止时间");
        }
        return query.search(request.keyword(), request.loginAccount(), request.displayName(), request.clientIp(),
                request.result(), request.reasonCode(), request.from(), request.to(), request.page(), request.size());
    }

    /** 根据不可变事实标识读取完整详情。 */
    @GetMapping("/{id}")
    @Operation(summary = "查询登录审计详情")
    public LoginAuditDto detail(@PathVariable String id) {
        if (!id.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审计 ID 必须是大写 ULID");
        }
        return query.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "登录审计不存在"));
    }
}
