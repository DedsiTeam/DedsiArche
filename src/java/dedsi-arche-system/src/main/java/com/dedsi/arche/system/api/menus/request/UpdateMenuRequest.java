package com.dedsi.arche.system.api.menus.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.dedsi.arche.system.domain.menus.MenuType;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 修改菜单节点请求。 */
public record UpdateMenuRequest(
        /* 当前根版本，必填。 */
        @Schema(description = "当前根版本，必填")
        @NotNull @Min(0) Long version,
        /* 上级目录，null 顶级，字段必须出现。 */
        @Schema(description = "上级目录，null 顶级，字段必须出现")
        @Size(max = 26) @JsonProperty(required = true) String parentId,
        /* 显示名称，null 保持。 */
        @Schema(description = "显示名称，null 保持")
        @Size(max = 100) String name,
        /* 节点类型，null 保持。 */
        @Schema(description = "节点类型，null 保持")
        MenuType menuType,
        /* 权限标识，null 解除绑定，字段必须出现。 */
        @Schema(description = "权限标识，null 解除绑定，字段必须出现")
        @Size(max = 26) @JsonProperty(required = true) String permissionId,
        /* PAGE路径最多500字符或HTTP(S)外链最多2048字符，空白清除。 */
        @Schema(description = "PAGE路径最多500字符或HTTP(S)外链最多2048字符，空白清除")
        @Size(max = 2048) String routePath,
        /* 逻辑组件路径，空白清除。 */
        @Schema(description = "逻辑组件路径，空白清除")
        @Size(max = 255) String componentPath,
        /* 非负排序。 */
        @Schema(description = "非负排序")
        @Min(0) Integer sortOrder,
        /* 可空说明，空白清除。 */
        @Schema(description = "可空说明，空白清除")
        @Size(max = 500) String description,
        /* 是否显示，创建默认 true。 */
        @Schema(description = "是否显示，创建默认 true")
        Boolean visible,
        /* 是否启用，创建默认 true。 */
        @Schema(description = "是否启用，创建默认 true")
        Boolean enabled,
        /* 是否缓存，创建 PAGE 默认 true，其他 false。 */
        @Schema(description = "是否缓存，创建 PAGE 默认 true，其他 false")
        Boolean cache,
        /* 是否固定标签，创建默认 false。 */
        @Schema(description = "是否固定标签，创建默认 false")
        Boolean affix
) {
}
