package com.dedsi.arche.system.api.users;
import com.dedsi.arche.common.exception.BusinessException;
import com.dedsi.arche.system.domain.users.*;
import com.dedsi.arche.system.domain.users.query.UserQuery;
import com.dedsi.arche.system.api.users.request.*;
import com.dedsi.arche.system.api.users.response.*;
import com.dedsi.arche.system.infrastructure.users.security.TokenService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
/** 用例编排、事务边界和 HTTP 转换均在 API 层完成。 */
@Tag(name="用户", description="用户注册、认证及个人资料")
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    private final UserQuery query;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final String dummyHash;
    public UserController(UserRepository users,UserQuery query,PasswordEncoder passwords,TokenService tokens) {
        this.users=users; this.query=query; this.passwords=passwords; this.tokens=tokens;
        dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    @Operation(summary="注册用户", description="用户名为 3～50 位英文字母、数字或下划线；密码至少 12 字符，UTF-8 最多 72 字节。")
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        if(request.password().getBytes(StandardCharsets.UTF_8).length>72)
            throw new BusinessException("PASSWORD_TOO_LONG","密码 UTF-8 编码最多 72 字节");
        if(users.findByUsername(User.normalizeUsername(request.username())).isPresent())
            throw new BusinessException("USERNAME_EXISTS","用户名已存在");
        User user=users.save(User.register(request.username(),request.displayName(),passwords.encode(request.password())));
        return response(user);
    }
    @Operation(summary="账号密码登录", description="返回有效期 15 分钟的 JWT 和有效期 7 天的刷新令牌。响应不得缓存。")
    @PostMapping("/login") @Transactional
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user=users.findByUsername(User.normalizeUsername(request.username())).orElse(null);
        boolean matches=request.password().getBytes(StandardCharsets.UTF_8).length<=72 && passwords.matches(request.password(),user==null?dummyHash:user.getPasswordHash());
        if(user==null || !matches) throw new BadCredentialsException("用户名或密码错误");
        user.ensureEnabled();
        return loginResponse(user,tokens.issue(user.getId()));
    }
    @Operation(summary="刷新登录凭据", description="每次使用后旧刷新令牌失效，请保存响应中的新刷新令牌。")
    @PostMapping("/refresh") @Transactional
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        UUID id=tokens.consume(request.refreshToken());
        User user=users.findById(id).orElseThrow(()->new BadCredentialsException("用户不存在"));
        user.ensureEnabled();
        return loginResponse(user,tokens.issue(id));
    }
    @Operation(summary="退出登录", description="撤销给定刷新令牌；已签发的 JWT 在过期前仍有效。")
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void logout(@Valid @RequestBody RefreshTokenRequest request) { tokens.revoke(request.refreshToken()); }
    @Operation(summary="查询当前用户")
    @SecurityRequirement(name="bearerAuth")
    @GetMapping("/me") @PreAuthorize("hasAuthority('SCOPE_profile')") @Transactional(readOnly=true)
    public UserResponse me(@Parameter(hidden=true) @AuthenticationPrincipal Jwt jwt) {
        var user=query.findById(UUID.fromString(jwt.getSubject())).orElseThrow(()->new BadCredentialsException("用户不存在"));
        return new UserResponse(user.id(),user.username(),user.displayName());
    }
    @Operation(summary="修改当前用户显示名称", description="事务内修改受管理实体，由 Hibernate 脏检查自动持久化。")
    @SecurityRequirement(name="bearerAuth")
    @PatchMapping("/me") @PreAuthorize("hasAuthority('SCOPE_profile')") @Transactional
    public UserResponse rename(@Parameter(hidden=true) @AuthenticationPrincipal Jwt jwt,@Valid @RequestBody UpdateProfileRequest request) {
        User user=users.findById(UUID.fromString(jwt.getSubject())).orElseThrow(()->new BadCredentialsException("用户不存在"));
        user.ensureEnabled(); user.rename(request.displayName());
        // 不调用 save：当前事务中的受管理实体由 Hibernate 脏检查持久化。
        return response(user);
    }
    private UserResponse response(User user) { return new UserResponse(user.getId(),user.getUsername(),user.getDisplayName()); }
    private ResponseEntity<LoginResponse> loginResponse(User user,TokenService.Tokens issued) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new LoginResponse(issued.accessToken(),"Bearer",issued.expiresAt(),issued.refreshToken(),response(user)));
    }
}
