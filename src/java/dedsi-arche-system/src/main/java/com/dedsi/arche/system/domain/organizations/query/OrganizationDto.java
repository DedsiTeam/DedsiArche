package com.dedsi.arche.system.domain.organizations.query;

import com.dedsi.arche.system.domain.organizations.Organization;
import java.time.LocalDateTime;

/** 组织详情及分页节点只读响应。 */
public record OrganizationDto(
        /* 组织 ULID。 */
        String id,
        /* 不可变所属系统编码。 */
        String systemCode,
        /* 同步维护的系统名称。 */
        String systemName,
        /* 上级组织标识，null 为顶级。 */
        String parentId,
        /* 系统内唯一且不可变的编码。 */
        String code,
        /* 机构名称。 */
        String name,
        /* 备用名称 1，可空。 */
        String name1,
        /* 备用名称 2，可空。 */
        String name2,
        /* 备用名称 3，可空。 */
        String name3,
        /* 备用名称 4，可空。 */
        String name4,
        /* 非负同层排序。 */
        int sortOrder,
        /* 可空说明。 */
        String description,
        /* 是否启用。 */
        boolean enabled,
        /* 北京时间创建时间。 */
        LocalDateTime createdAt,
        /* 北京时间修改时间。 */
        LocalDateTime updatedAt,
        /* 乐观并发版本。 */
        Long version
) {
    /** 从独立节点构造响应。 */
    public static OrganizationDto from(Organization value) {
        return new OrganizationDto(
                value.getId(), value.getSystemCode(), value.getSystemName(), value.getParentId(),
                value.getCode(), value.getName(), value.getName1(), value.getName2(),
                value.getName3(), value.getName4(), value.getSortOrder(), value.getDescription(),
                value.isEnabled(), value.getCreatedAt(), value.getUpdatedAt(), value.getVersion()
        );
    }
}
