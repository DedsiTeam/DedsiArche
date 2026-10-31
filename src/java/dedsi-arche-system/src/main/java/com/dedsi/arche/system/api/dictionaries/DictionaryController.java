package com.dedsi.arche.system.api.dictionaries;

import com.dedsi.arche.common.exception.BusinessException;
import com.dedsi.arche.system.api.dictionaries.request.*;
import com.dedsi.arche.system.domain.dictionaries.Dictionary;
import com.dedsi.arche.system.domain.dictionaries.DictionaryItemInput;
import com.dedsi.arche.system.domain.dictionaries.IDictionaryRepository;
import com.dedsi.arche.system.domain.dictionaries.query.DictionaryDto;
import com.dedsi.arche.system.domain.dictionaries.query.DictionaryPage;
import com.dedsi.arche.system.domain.dictionaries.query.DictionaryOptionDto;
import com.dedsi.arche.system.domain.dictionaries.query.IDictionaryQuery;
import com.dedsi.arche.system.domain.systems.IManagedSystemRepository;
import com.dedsi.arche.system.domain.systems.ManagedSystem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Objects;

/** 字典管理与登录用户业务读取；管理操作复用服务器端管理员鉴权。 */
@RestController
@RequestMapping("/api/dictionaries")
@Tag(name = "字典管理")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@userAdministration.canManage(authentication)")
public class DictionaryController {
    /* 完整字典聚合仓储。 */
    private final IDictionaryRepository dictionaries;
    /* 分页只读投影。 */
    private final IDictionaryQuery query;
    /* 系统名称来源与写入协调锁。 */
    private final IManagedSystemRepository systems;
    /* 系统锁等待结束后刷新已加载的完整聚合，避免幂等更新跳过并发版本检查。 */
    private final EntityManager em;

    /** 注入聚合、投影及系统仓储。 */
    public DictionaryController(
            IDictionaryRepository dictionaries, IDictionaryQuery query,
            IManagedSystemRepository systems, EntityManager em
    ) {
        this.dictionaries = dictionaries;
        this.query = query;
        this.systems = systems;
        this.em = em;
    }

    /** 在一个事务内创建根和全部子项。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建字典", description = "所属系统与字典编码联合唯一且不可修改；默认启用。子项默认排序 0、非默认、启用，ID 必须为空。")
    @Transactional
    public DictionaryDto create(@Valid @RequestBody CreateDictionaryRequest request) {
        var system = lockSystem(request.systemCode().trim());
        if (dictionaries.existsBySystemCodeAndCode(system.getCode(), request.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该系统下字典编码已存在");
        }
        var dictionary = dictionaries.save(Dictionary.create(
                system.getCode(), system.getName(), request.code(), request.name(), request.description(),
                request.enabled() == null || request.enabled(), inputs(request.items(), true)
        ));
        dictionaries.flush();
        return DictionaryDto.from(dictionary);
    }

    /** 完整读取根和全部子项，包括停用项。 */
    @GetMapping("/{id}")
    @Operation(summary = "查询字典详情", description = "返回完整子项集合，包括停用项；按 sortOrder、code、id 排序。")
    @Transactional(readOnly = true)
    public DictionaryDto detail(@PathVariable String id) {
        return DictionaryDto.from(find(id));
    }

    /** 业务读取允许所有已登录用户，但只返回启用字典中的启用项。 */
    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "读取业务可用字典", description = "已登录即可读取。不存在或字典停用返回 404；成功仅返回启用子项，按 sortOrder、code、id 排序。")
    @Transactional(readOnly = true)
    public DictionaryDto available(
            @RequestParam @NotBlank @Size(max = 100) String systemCode,
            @RequestParam @NotBlank @Size(max = 100) String code
    ) {
        var dictionary = dictionaries.findBySystemCodeAndCode(systemCode.trim(), code.trim())
                .filter(Dictionary::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "字典不存在或已停用"));
        return DictionaryDto.from(dictionary, true);
    }

    /** 按字典编码和所属系统读取全部下拉项。 */
    @GetMapping("/options/{code}/{systemCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "读取字典下拉选项", description = "路径参数 code、systemCode 均必填，联合定位字典。返回全部字典项的 code、name，包括停用字典及停用项，按 sortOrder、code、id 排序。不存在返回 404，空字典返回空数组。")
    @Transactional(readOnly = true)
    public List<DictionaryOptionDto> options(
            @PathVariable @NotBlank @Size(max = 100) String code,
            @PathVariable @NotBlank @Size(max = 100) String systemCode
    ) {
        var dictionary = dictionaries.findBySystemCodeAndCode(systemCode.trim(), code.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "字典不存在"));
        // 聚合公开集合已按 sortOrder、code、id 排序，此处不按启用状态过滤。
        return dictionary.getItems().stream()
                .map(item -> new DictionaryOptionDto(item.getCode(), item.getName()))
                .toList();
    }

    /** 返回稳定排序的分页摘要，不加载子项。 */
    @PostMapping("/page")
    @Operation(summary = "分页查询字典", description = "按编码或名称搜索，可按系统和启用状态筛选；页码从 1 开始，默认 10 条，上限 100。")
    public DictionaryPage page(@Valid @RequestBody DictionaryPageRequest request) {
        return query.search(request.keyword(), request.systemCode(), request.enabled(), request.page(), request.size());
    }

    /** 同事务修改根和完整子项集合，旧版本不能覆盖新资料。 */
    @PatchMapping("/{id}")
    @Operation(summary = "编辑字典", description = "version 必填。根字段 null 保持，说明空白清除；items null 保持、空数组清空、非空全量替换。已有子项 ID 与编码不可改变；最多一个默认项且必须启用。")
    @Transactional
    public DictionaryDto update(@PathVariable String id, @Valid @RequestBody UpdateDictionaryRequest request) {
        var dictionary = find(id);
        lockSystem(dictionary.getSystemCode());
        refresh(dictionary);
        checkVersion(dictionary, request.version());
        dictionary.update(
                request.name() == null ? dictionary.getName() : request.name(),
                request.description() == null ? dictionary.getDescription() : request.description(),
                request.enabled() == null ? dictionary.isEnabled() : request.enabled(),
                request.items() == null ? null : inputs(request.items(), false)
        );
        dictionaries.flush();
        return DictionaryDto.from(dictionary);
    }

    /** 删除整个字典，级联删除子项但保留系统目录。 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除字典", description = "提交当前 version；成功删除根及全部子项，不删除所属系统。")
    @Transactional
    public void delete(@PathVariable String id, @RequestParam @Min(0) long version) {
        var dictionary = find(id);
        lockSystem(dictionary.getSystemCode());
        refresh(dictionary);
        checkVersion(dictionary, version);
        dictionaries.delete(dictionary);
        dictionaries.flush();
    }

    /** 在任何字典写入前取得系统锁，协调系统更名和删除。 */
    private ManagedSystem lockSystem(String code) {
        return systems.lockByCode(code).orElseThrow(() ->
                new BusinessException("DICTIONARY_SYSTEM_NOT_FOUND", "所属系统不存在，请先登记系统"));
    }

    /** 等待系统锁期间根或子项可能已删除；实体缺失映射为并发冲突，其他异常继续使事务回滚。 */
    private void refresh(Dictionary dictionary) {
        try {
            em.refresh(dictionary);
        } catch (EntityNotFoundException exception) {
            // 级联刷新也可能遇到已移除的旧子项，不能断言字典根已被删除。
            throw new ResponseStatusException(HttpStatus.CONFLICT, "字典或字典项已变化，请重新读取后提交");
        }
    }

    /** 创建禁止客户端指定子项标识；领域层继续验证全部集合不变量。 */
    private List<DictionaryItemInput> inputs(List<DictionaryItemRequest> items, boolean creating) {
        if (items == null) {
            return List.of();
        }
        if (creating && items.stream().anyMatch(item -> item.id() != null)) {
            throw new BusinessException("INVALID_DICTIONARY", "创建字典时不能指定子项 ID");
        }
        return items.stream().map(DictionaryItemRequest::toInput).toList();
    }

    /** ULID 格式错误与记录不存在分别返回 400、404。 */
    private Dictionary find(String id) {
        if (!id.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw new BusinessException("INVALID_DICTIONARY", "字典 ID 必须是大写 ULID");
        }
        return dictionaries.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "字典不存在"));
    }

    /** 提前拒绝旧版本，数据库根乐观锁继续兜底等待锁期间发生的并发修改。 */
    private void checkVersion(Dictionary dictionary, long version) {
        if (!Objects.equals(dictionary.getVersion(), version)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "字典已被修改，请刷新后重试");
        }
    }
}
