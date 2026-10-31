package com.dedsi.arche.system.api.organizations.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 编辑组织节点请求。 */
public record UpdateOrganizationRequest(
        /* 当前根版本，必填。 */
        @Schema(description = "当前根版本，必填")
        @NotNull @Min(0) Long version,
        /* 上级组织标识，null 为顶级，字段必须出现。 */
        @Schema(description = "上级组织标识，null 为顶级，字段必须出现")
        @Size(max = 26) @JsonProperty(required = true) String parentId,
        /* 名称，null 保持。 */
        @Schema(description = "名称，null 保持")
        @Size(max = 100) String name,
        /* 备用名称 1，空白清除，null 保持。 */
        @Schema(description = "备用名称 1，空白清除，null 保持")
        @Size(max = 100) String name1,
        /* 备用名称 2，空白清除，null 保持。 */
        @Schema(description = "备用名称 2，空白清除，null 保持")
        @Size(max = 100) String name2,
        /* 备用名称 3，空白清除，null 保持。 */
        @Schema(description = "备用名称 3，空白清除，null 保持")
        @Size(max = 100) String name3,
        /* 备用名称 4，空白清除，null 保持。 */
        @Schema(description = "备用名称 4，空白清除，null 保持")
        @Size(max = 100) String name4,
        /* 非负排序，null 保持。 */
        @Schema(description = "非负排序，null 保持")
        @Min(0) Integer sortOrder,
        /* 说明，空白清除，null 保持。 */
        @Schema(description = "说明，空白清除，null 保持")
        @Size(max = 500) String description,
        /* 启用状态，null 保持。 */
        @Schema(description = "启用状态，null 保持")
        Boolean enabled
) {
}
