package com.dedsi.arche.system.api.menus.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.dedsi.arche.system.domain.menus.MenuType;

/** 新增菜单节点请求。 */
public record CreateMenuRequest(
        /* 所属系统编码。 */
        @Schema(description = "所属系统编码")
        @NotBlank @Size(max = 100) String systemCode,
        /* 上级目录，null 顶级。 */
        @Schema(description = "上级目录，null 顶级")
        @Size(max = 26) String parentId,
        /* 系统内唯一且不可变编码。 */
        @Schema(description = "系统内唯一且不可变编码")
        @NotBlank @Size(max = 100) String code,
        /* 显示名称，null 不可用。 */
        @Schema(description = "显示名称，null 不可用")
        @NotBlank @Size(max = 100) String name,
        /* 节点类型，必填。 */
        @Schema(description = "节点类型，必填")
        @NotNull MenuType menuType,
        /* 权限标识，null 解除绑定。 */
        @Schema(description = "权限标识，null 解除绑定")
        @Size(max = 26) String permissionId,
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
