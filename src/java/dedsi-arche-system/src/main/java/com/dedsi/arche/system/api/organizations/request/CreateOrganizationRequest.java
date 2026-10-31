package com.dedsi.arche.system.api.organizations.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

/** 新增组织节点请求。 */
public record CreateOrganizationRequest(
        /* 已登记所属系统编码。 */
        @Schema(description = "已登记所属系统编码")
        @NotBlank @Size(max = 100) String systemCode,
        /* 上级组织标识，null 为顶级。 */
        @Schema(description = "上级组织标识，null 为顶级")
        @Size(max = 26) String parentId,
        /* 系统内唯一且不可修改的组织编码。 */
        @Schema(description = "系统内唯一且不可修改的组织编码")
        @NotBlank @Size(max = 100) String code,
        /* 组织名称，必填。 */
        @Schema(description = "组织名称，必填")
        @NotBlank @Size(max = 100) String name,
        /* 备用名称 1，空白清除，null 默认空。 */
        @Schema(description = "备用名称 1，空白清除，null 默认空")
        @Size(max = 100) String name1,
        /* 备用名称 2，空白清除，null 默认空。 */
        @Schema(description = "备用名称 2，空白清除，null 默认空")
        @Size(max = 100) String name2,
        /* 备用名称 3，空白清除，null 默认空。 */
        @Schema(description = "备用名称 3，空白清除，null 默认空")
        @Size(max = 100) String name3,
        /* 备用名称 4，空白清除，null 默认空。 */
        @Schema(description = "备用名称 4，空白清除，null 默认空")
        @Size(max = 100) String name4,
        /* 非负排序，null 默认 0。 */
        @Schema(description = "非负排序，null 默认 0")
        @Min(0) Integer sortOrder,
        /* 说明，空白清除，null 默认空。 */
        @Schema(description = "说明，空白清除，null 默认空")
        @Size(max = 500) String description,
        /* 启用状态，null 默认 true。 */
        @Schema(description = "启用状态，null 默认 true")
        Boolean enabled
) {
}
