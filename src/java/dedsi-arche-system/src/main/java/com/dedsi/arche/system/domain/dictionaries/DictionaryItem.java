package com.dedsi.arche.system.domain.dictionaries;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import java.util.Objects;

/** 字典聚合拥有的子实体；只能通过字典整体配置维护。 */
@Entity
@Table(name = "sys_dictionary_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_dictionary_items_code", columnNames = {"dictionary_id", "code"}))
public class DictionaryItem {
    /* 字典项标识，26 位 ULID。 */
    @Id
    @Column(length = 26, nullable = false)
    private String id;

    /* 所属字典，不允许跨聚合转移。 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dictionary_id", nullable = false, updatable = false)
    private Dictionary dictionary;

    /* 字典内唯一编码，创建后不可修改。 */
    @Column(length = 100, nullable = false, updatable = false)
    private String code;

    /* 展示名称。 */
    @Column(length = 100, nullable = false)
    private String name;

    /* 非负展示排序。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /* 默认项标记，必须同时启用。 */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    /* 字典项自身的启用状态。 */
    @Column(nullable = false)
    private boolean enabled;

    /** 供 JPA 使用。 */
    protected DictionaryItem() {
    }

    /** 接受聚合已经完成校验的配置，创建新子项。 */
    static DictionaryItem create(Dictionary dictionary, DictionaryItemInput input) {
        var item = new DictionaryItem();
        item.id = UlidCreator.getUlid().toString();
        item.dictionary = dictionary;
        item.code = input.code();
        item.update(input);
        return item;
    }

    /** 应用已校验的可变字段并报告是否确有变化。 */
    boolean update(DictionaryItemInput input) {
        boolean changed = !Objects.equals(name, input.name())
                || sortOrder != input.sortOrder()
                || isDefault != input.isDefault()
                || enabled != input.enabled();
        name = input.name();
        sortOrder = input.sortOrder();
        isDefault = input.isDefault();
        enabled = input.enabled();
        return changed;
    }

    public String getId() {
        return id;
    }

    public String getDictionaryId() {
        return dictionary.getId();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
