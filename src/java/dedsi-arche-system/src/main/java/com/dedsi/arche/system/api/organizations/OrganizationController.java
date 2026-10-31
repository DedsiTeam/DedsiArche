package com.dedsi.arche.system.api.organizations;

import com.dedsi.arche.common.exception.BusinessException;
import com.dedsi.arche.system.api.organizations.request.*;
import com.dedsi.arche.system.domain.organizations.Organization;
import com.dedsi.arche.system.domain.organizations.IOrganizationRepository;
import com.dedsi.arche.system.domain.organizations.query.*;
import com.dedsi.arche.system.domain.systems.IManagedSystemRepository;
import com.dedsi.arche.system.domain.systems.ManagedSystem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 组织管理编排，系统锁串行化同系统的结构与启停变更。 */
@RestController
@RequestMapping("/api/organizations")
@Tag(name = "组织机构管理")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@userAdministration.canManage(authentication)")
public class OrganizationController {
    /* 独立组织节点聚合仓储。 */
    private final IOrganizationRepository organizations;
    /* 树及分页只读查询。 */
    private final IOrganizationQuery query;
    /* 所属系统与树结构写入协调锁。 */
    private final IManagedSystemRepository systems;
    /* 等待系统锁后刷新目标，保证版本预检基于最新状态。 */
    private final EntityManager em;

    /** 注入事务编排依赖。 */
    public OrganizationController(
            IOrganizationRepository organizations, IOrganizationQuery query,
            IManagedSystemRepository systems, EntityManager em
    ) {
        this.organizations = organizations;
        this.query = query;
        this.systems = systems;
        this.em = em;
    }

    /** 创建独立节点，上级为空时为顶级。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建组织", description = "所属系统与编码不可修改；上级必须同系统，启用节点的上级必须启用。")
    @Transactional
    public OrganizationDto create(@Valid @RequestBody CreateOrganizationRequest request) {
        var system = lockSystem(request.systemCode().trim());
        if (organizations.existsBySystemCodeAndCode(system.getCode(), request.code().trim())) {
            throw conflict("该系统下组织编码已存在");
        }
        boolean enabled = request.enabled() == null || request.enabled();
        validateParent(null, system.getCode(), request.parentId(), enabled);
        var organization = organizations.save(Organization.create(
                system.getCode(), system.getName(), request.parentId(), request.code(), request.name(),
                request.name1(), request.name2(), request.name3(), request.name4(),
                request.sortOrder() == null ? 0 : request.sortOrder(), request.description(), enabled
        ));
        organizations.flush();
        return OrganizationDto.from(organization);
    }

    /** 读取单一组织节点。 */
    @GetMapping("/{id}")
    @Operation(summary = "查询组织详情")
    @Transactional(readOnly = true)
    public OrganizationDto detail(@PathVariable String id) {
        return OrganizationDto.from(find(id));
    }

    /** 完整树包含停用节点，每层按排序值、编码及标识排序。 */
    @GetMapping("/tree/{systemCode}")
    @Operation(summary = "查询组织完整树", description = "返回根节点数组，每个节点附 children；包含停用节点，无节点时为空数组。")
    public List<OrganizationTreeDto> tree(@PathVariable @NotBlank @Size(max = 100) String systemCode) {
        return query.tree(systemCode.trim());
    }

    /** 分页结果为独立节点，不因筛选丢失上级而构造残缺树。 */
    @PostMapping("/page")
    @Operation(summary = "分页查询组织")
    public OrganizationPage page(@Valid @RequestBody OrganizationPageRequest request) {
        return query.search(request.keyword(), request.systemCode(), request.enabled(), request.page(), request.size());
    }

    /** 父标识必须明确提交；移动节点不会修改后代的父关系。 */
    @PatchMapping("/{id}")
    @Operation(summary = "编辑组织", description = "version、parentId 字段必填，parentId=null 移为顶级；其他字段 null 保持，备用名称和说明空白清除。禁止环和跨系统上级。")
    @Transactional
    public OrganizationDto update(@PathVariable String id, @Valid @RequestBody UpdateOrganizationRequest request) {
        var organization = find(id);
        lockSystem(organization.getSystemCode());
        refresh(organization);
        checkVersion(organization, request.version());
        boolean enabled = request.enabled() == null ? organization.isEnabled() : request.enabled();
        validateParent(id, organization.getSystemCode(), request.parentId(), enabled);
        if (!enabled && organizations.existsByParentIdAndEnabledTrue(id)) {
            throw conflict("存在启用的下级组织，不能停用");
        }
        organization.update(
                request.parentId(), keep(request.name(), organization.getName()),
                keep(request.name1(), organization.getName1()), keep(request.name2(), organization.getName2()),
                keep(request.name3(), organization.getName3()), keep(request.name4(), organization.getName4()),
                request.sortOrder() == null ? organization.getSortOrder() : request.sortOrder(),
                keep(request.description(), organization.getDescription()), enabled
        );
        organizations.flush();
        return OrganizationDto.from(organization);
    }

    /** 仅允许删除叶子；停用子节点同样阻止删除。 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除叶子组织", description = "version 必填，有任何直属子节点时返回 409。")
    @Transactional
    public void delete(@PathVariable String id, @RequestParam @Min(0) long version) {
        var organization = find(id);
        lockSystem(organization.getSystemCode());
        refresh(organization);
        checkVersion(organization, version);
        if (organizations.existsByParentId(id)) {
            throw conflict("组织下存在子节点，不能删除");
        }
        organizations.delete(organization);
        organizations.flush();
    }

    /** 所有树结构校验在系统锁内完成，避免并发互相移动导致环。 */
    private void validateParent(String id, String systemCode, String parentId, boolean enabled) {
        if (parentId == null) {
            return;
        }
        validateId(parentId);
        var visited = new HashSet<String>();
        String cursor = parentId;
        boolean directParent = true;
        while (cursor != null) {
            if (Objects.equals(cursor, id) || !visited.add(cursor)) {
                throw invalid("上级组织不能是自身或后代");
            }
            var parent = organizations.findById(cursor).orElseThrow(() -> invalid("上级组织不存在"));
            if (!systemCode.equals(parent.getSystemCode())) {
                throw invalid("上级组织必须属于同一系统");
            }
            if (directParent && enabled && !parent.isEnabled()) {
                throw invalid("启用组织的上级必须启用");
            }
            directParent = false;
            cursor = parent.getParentId();
        }
    }

    /** 所属系统锁与系统改名、删除使用相同锁顺序。 */
    private ManagedSystem lockSystem(String code) {
        return systems.lockByCode(code).orElseThrow(() ->
                new BusinessException("ORGANIZATION_SYSTEM_NOT_FOUND", "所属系统不存在，请先登记系统"));
    }

    /** 等待系统锁时被删除的节点视为并发冲突。 */
    private void refresh(Organization organization) {
        try {
            em.refresh(organization);
        } catch (EntityNotFoundException exception) {
            throw conflict("组织已被修改，请刷新后重试");
        }
    }

    /** 格式错误与不存在分别返回 400、404。 */
    private Organization find(String id) {
        validateId(id);
        return organizations.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "组织不存在"));
    }

    private void validateId(String id) {
        if (!id.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw invalid("组织 ID 必须是大写 ULID");
        }
    }

    /** 无变化请求也必须先通过最新版本检查。 */
    private void checkVersion(Organization organization, long version) {
        if (!Objects.equals(organization.getVersion(), version)) {
            throw conflict("组织已被修改，请刷新后重试");
        }
    }

    private String keep(String incoming, String current) {
        return incoming == null ? current : incoming;
    }

    private BusinessException invalid(String message) {
        return new BusinessException("INVALID_ORGANIZATION", message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
