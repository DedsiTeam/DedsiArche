package com.dedsi.arche.system.api.permissions.request;

import jakarta.validation.constraints.*;

/**
 * 新增权限请求；启用状态和排序值可省略，由接口赋默认值。
 */
public record CreatePermissionRequest(
        /* 所属系统编码，与权限编码共同构成唯一键，创建后不可修改。 */
        @NotBlank @Size(max = 100) String systemCode,
        /* 系统名称由服务端读取，此字段不参与赋值。 */
        @Size(max = 100) String systemName,
        /* 权限编码，在同一系统内唯一，创建后不可修改。 */
        @NotBlank @Size(max = 100) String code,
        /* 权限展示名称。 */
        @NotBlank @Size(max = 100) String name,
        /* 权限说明，允许为空。 */
        @Size(max = 500) String description,
        /* 权限定义是否启用；当前不承担用户授权判定。 */
        Boolean enabled,
        /* 展示顺序，非负数，值越小越靠前。 */
        @Min(0) Integer sortOrder
) {
}
