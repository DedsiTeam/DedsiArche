package com.dedsi.arche.system.domain.dictionaries;

import com.dedsi.arche.common.exception.BusinessException;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 字典聚合根；字典项随根整存整取，共享根的并发版本。 */
@Entity
@Table(name = "sys_dictionaries", uniqueConstraints = @UniqueConstraint(
        name = "uk_dictionaries_system_code", columnNames = {"system_code", "code"}))
public class Dictionary {
    /* 字典标识，26 位 ULID。 */
    @Id
    @Column(length = 26, nullable = false)
    private String id;

    /* 所属已登记系统编码，创建后不可修改。 */
    @Column(name = "system_code", length = 100, nullable = false, updatable = false)
    private String systemCode;

    /* 服务端读取的系统当前名称，通过同步领域事件维护。 */
    @Column(name = "system_name", length = 100, nullable = false)
    private String systemName;

    /* 系统内唯一编码，区分大小写且创建后不可修改。 */
    @Column(length = 100, nullable = false, updatable = false)
    private String code;

    /* 字典名称。 */
    @Column(length = 100, nullable = false)
    private String name;

    /* 可选说明，最多 500 字符。 */
    @Column(length = 500)
    private String description;

    /* 字典整体启用状态，不改变子项原有配置。 */
    @Column(nullable = false)
    private boolean enabled;

    /* 创建时间，北京时间。 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* 修改时间，北京时间；子项变化也推进以触发根版本检查。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* 整个聚合的乐观锁版本。 */
    @Version
    @Column(nullable = false)
    private Long version;

    /* 完整字典项集合，包括停用项；移除项时删除子实体。 */
    @OneToMany(mappedBy = "dictionary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, code ASC, id ASC")
    private List<DictionaryItem> items = new ArrayList<>();

    /** 供 JPA 使用。 */
    protected Dictionary() {
    }

    /** 创建字典；系统资料由应用从系统目录读取，允许空字典。 */
    public static Dictionary create(
            String systemCode, String systemName, String code, String name,
            String description, boolean enabled, List<DictionaryItemInput> items
    ) {
        var dictionary = new Dictionary();
        dictionary.systemCode = required(systemCode, "系统编码");
        dictionary.systemName = required(systemName, "系统名称");
        dictionary.code = required(code, "字典编码");
        dictionary.id = UlidCreator.getUlid().toString();
        dictionary.createdAt = now();
        dictionary.updatedAt = dictionary.createdAt;
        dictionary.update(name, description, enabled, items);
        dictionary.updatedAt = dictionary.createdAt;
        return dictionary;
    }

    /** 先校验最终状态再整体变更；null 集合保持原配置，空集合清空。 */
    public void update(
            String name, String description, boolean enabled, List<DictionaryItemInput> nextItems
    ) {
        String nextName = required(name, "字典名称");
        if (description != null && description.length() > 500) {
            throw invalid("字典说明最多 500 字符");
        }
        String nextDescription = description == null || description.isBlank() ? null : description.trim();
        List<DictionaryItemInput> normalized = validateItems(nextItems);
        boolean changed = !Objects.equals(this.name, nextName)
                || !Objects.equals(this.description, nextDescription) || this.enabled != enabled;
        this.name = nextName;
        this.description = nextDescription;
        this.enabled = enabled;
        if (normalized != null) {
            var existing = new HashMap<String, DictionaryItem>();
            for (var item : items) {
                existing.put(item.getId(), item);
            }
            var retained = new HashSet<String>();
            for (var input : normalized) {
                if (input.id() == null) {
                    items.add(DictionaryItem.create(this, input));
                    changed = true;
                } else {
                    retained.add(input.id());
                    changed |= existing.get(input.id()).update(input);
                }
            }
            changed |= items.removeIf(item -> existing.containsKey(item.getId()) && !retained.contains(item.getId()));
        }
        if (changed) {
            touch();
        }
    }

    /** 验证最终集合的归属、不可变编码、唯一性与默认项约束，不提前修改实体。 */
    private List<DictionaryItemInput> validateItems(List<DictionaryItemInput> inputs) {
        if (inputs == null) {
            return null;
        }
        var existing = new HashMap<String, DictionaryItem>();
        var existingCodes = new HashSet<String>();
        for (var item : items) {
            existing.put(item.getId(), item);
            existingCodes.add(item.getCode());
        }
        var ids = new HashSet<String>();
        var codes = new HashSet<String>();
        var normalized = new ArrayList<DictionaryItemInput>();
        int defaults = 0;
        for (var input : inputs) {
            if (input == null) {
                throw invalid("字典项不能为空");
            }
            String itemCode = required(input.code(), "字典项编码");
            String itemName = required(input.name(), "字典项名称");
            if (!codes.add(itemCode)) {
                throw invalid("字典项编码不能重复");
            }
            if (input.sortOrder() < 0) {
                throw invalid("字典项排序不能小于零");
            }
            if (input.isDefault() && (!input.enabled() || ++defaults > 1)) {
                throw invalid("最多只能设置一个默认项，且默认项必须启用");
            }
            if (input.id() == null) {
                if (existingCodes.contains(itemCode)) {
                    throw invalid("已有字典项编码必须保留原字典项标识，不能删除后重新新增同编码项");
                }
            } else {
                var item = existing.get(input.id());
                if (item == null || !ids.add(input.id())) {
                    throw invalid("字典项标识不属于当前字典或重复提交");
                }
                if (!item.getCode().equals(itemCode)) {
                    throw invalid("字典项编码创建后不可修改");
                }
            }
            normalized.add(new DictionaryItemInput(
                    input.id(), itemCode, itemName, input.sortOrder(), input.isDefault(), input.enabled()));
        }
        return normalized;
    }

    /** 系统名称事件仅同步当前归属系统，实际变化时推进聚合版本。 */
    public void synchronizeSystemName(String systemCode, String name) {
        String normalizedCode = required(systemCode, "系统编码");
        String normalizedName = required(name, "系统名称");
        if (this.systemCode.equals(normalizedCode) && !this.systemName.equals(normalizedName)) {
            systemName = normalizedName;
            touch();
        }
    }

    /** 按数据库微秒精度推进修改时间，保证子实体变化使根变脏。 */
    private void touch() {
        LocalDateTime current = now();
        updatedAt = updatedAt != null && !current.isAfter(updatedAt) ? updatedAt.plusNanos(1000) : current;
    }

    /** 获取北京时间并对齐数据库精度。 */
    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MICROS);
    }

    /** 校验并去除文本首尾空格，编码保留大小写。 */
    private static String required(String value, String label) {
        if (value == null || value.isBlank() || value.length() > 100 || value.trim().isEmpty()) {
            throw invalid(label + "不能为空且最多 100 字符");
        }
        return value.trim();
    }

    /** 统一领域校验错误。 */
    private static BusinessException invalid(String message) {
        return new BusinessException("INVALID_DICTIONARY", message);
    }

    public String getId() {
        return id;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    /** 返回稳定排序的只读快照，防止调用方绕过根修改集合。 */
    public List<DictionaryItem> getItems() {
        return items.stream().sorted(Comparator.comparingInt(DictionaryItem::getSortOrder)
                .thenComparing(DictionaryItem::getCode).thenComparing(DictionaryItem::getId)).toList();
    }
}
