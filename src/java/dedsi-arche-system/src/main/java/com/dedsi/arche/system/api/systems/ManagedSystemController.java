package com.dedsi.arche.system.api.systems;

import com.dedsi.arche.system.api.systems.request.*;
import com.dedsi.arche.system.domain.systems.*;
import com.dedsi.arche.system.domain.systems.query.*;
import com.dedsi.arche.system.domain.permissions.IPermissionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** 系统管理 API，沿用 User 的管理员校验、事务及 DTO 编排方式。 */
@RestController
@RequestMapping("/api/systems")
@Tag(name = "系统管理")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@userAdministration.canManage(authentication)")
public class ManagedSystemController {
    private final IManagedSystemRepository systems;
    private final IManagedSystemQuery query;
    private final IPermissionRepository permissions;
    private final EntityManager em;

    /** 注入仓储、只读查询和事务内刷新的实体管理器。 */
    public ManagedSystemController(
            IManagedSystemRepository systems,
            IManagedSystemQuery query,
            IPermissionRepository permissions,
            EntityManager em
    ) {
        this.systems = systems;
        this.query = query;
        this.permissions = permissions;
        this.em = em;
    }

    /** 提供管理页面的系统下拉选项，沿用类级管理员鉴权。 */
    @GetMapping("/options")
    @Operation(summary = "系统下拉选项", description = "返回全部系统的 code 和 name，按排序值及标识升序排列。")
    @Transactional(readOnly = true)
    public List<ManagedSystemOptionDto> options() {
        return query.options();
    }

    /** 创建系统，编码唯一；已有同编码权限的展示名称同步为当前名称。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增系统", description = "编码唯一且不可变；默认未上线，排序默认 0。")
    @Transactional
    public ManagedSystemDto create(@Valid @RequestBody CreateManagedSystemRequest request) {
        if (systems.existsByCode(request.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "系统编码已存在");
        }
        var system = systems.save(ManagedSystem.create(
                request.code(), request.name(), request.description(), request.entryUrl(),
                request.status() == null ? SystemStatus.NOT_LAUNCHED : request.status(),
                request.sortOrder() == null ? 0 : request.sortOrder()
        ));
        em.flush();
        permissions.synchronizeSystemName(system.getCode(), system.getName());
        return dto(system);
    }

    /** 读取系统详情。 */
    @GetMapping("/{id}")
    @Operation(summary = "系统详情")
    @Transactional(readOnly = true)
    public ManagedSystemDto detail(@PathVariable String id) {
        validateId(id);
        return dto(systems.findById(id).orElseThrow(this::notFound));
    }

    /** 分页契约与用户管理一致，按排序值及标识升序返回。 */
    @PostMapping("/page")
    @Operation(summary = "分页查询系统", description = "按编码或名称搜索，可按状态筛选；页码从 1 开始。")
    @Transactional(readOnly = true)
    public ManagedSystemPage page(@Valid @RequestBody ManagedSystemPageRequest request) {
        return query.search(request.keyword(), request.status(), request.page(), request.size());
    }

    /** 在同一事务内更名并同步权限冗余名称，同时推进权限版本以拒绝旧编辑。 */
    @PatchMapping("/{id}")
    @Operation(summary = "编辑系统", description = "version 必填；null/省略保留，说明和入口空串清除。正常状态必须有入口。")
    @Transactional
    public ManagedSystemDto update(
            @PathVariable String id,
            @Valid @RequestBody UpdateManagedSystemRequest request
    ) {
        var system = lock(id);
        checkVersion(system, request.version());
        String previousName = system.getName();
        system.update(
                request.name() == null ? system.getName() : request.name(),
                request.description() == null ? system.getDescription() : request.description(),
                request.entryUrl() == null ? system.getEntryUrl() : request.entryUrl(),
                request.status() == null ? system.getStatus() : request.status(),
                request.sortOrder() == null ? system.getSortOrder() : request.sortOrder()
        );
        em.flush();
        if (!previousName.equals(system.getName())) {
            permissions.synchronizeSystemName(system.getCode(), system.getName());
        }
        return dto(system);
    }

    /** 有关联权限时拒绝删除，避免留下无所属系统的权限。 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除系统", description = "提交当前 version；有关联权限时返回 409。")
    @Transactional
    public void delete(@PathVariable String id, @RequestParam @Min(0) long version) {
        var system = lock(id);
        checkVersion(system, version);
        if (permissions.existsBySystemCode(system.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "系统下存在权限，不能删除");
        }
        systems.delete(system);
        em.flush();
    }

    /** 写操作先锁系统行，权限写入也遵循此锁顺序。 */
    private ManagedSystem lock(String id) {
        validateId(id);
        return systems.lockById(id).orElseThrow(this::notFound);
    }

    /** 拒绝非规范 ULID，避免无效标识进入持久化查询。 */
    private void validateId(String id) {
        if (!id.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "系统 ID 必须是 26 位大写 ULID");
        }
    }

    /** 显式版本检查与 JPA 乐观锁共同防止并发覆盖。 */
    private void checkVersion(ManagedSystem system, long version) {
        if (system.getVersion() != version) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "系统已被修改，请刷新后重试");
        }
    }

    /** 统一系统不存在的状态码。 */
    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "系统不存在");
    }

    /** 在 API 层组装详情，与 User 的 DTO 编排位置一致。 */
    private ManagedSystemDto dto(ManagedSystem system) {
        return new ManagedSystemDto(
                system.getId(), system.getCode(), system.getName(), system.getDescription(),
                system.getEntryUrl(), system.getStatus(), system.getSortOrder(),
                system.getCreatedAt(), system.getUpdatedAt(), system.getVersion()
        );
    }
}
