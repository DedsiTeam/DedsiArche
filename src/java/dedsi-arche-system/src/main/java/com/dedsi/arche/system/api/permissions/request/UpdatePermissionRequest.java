package com.dedsi.arche.system.api.permissions.request;

import jakarta.validation.constraints.*;

/**
 * 完整更新可编辑字段；必须携带当前版本，空说明表示清除。
 */
public record UpdatePermissionRequest(
        /* 系统名称由服务端读取，此字段不参与赋值。 */
        @Size(max = 100) String systemName,
        /* 权限展示名称。 */
        @NotBlank @Size(max = 100) String name,
        /* 权限说明，允许为空。 */
        @Size(max = 500) String description,
        /* 权限定义是否启用；当前不承担用户授权判定。 */
        @NotNull Boolean enabled,
        /* 展示顺序，非负数，值越小越靠前。 */
        @NotNull @Min(0) Integer sortOrder,
        /* 乐观锁版本，更新和删除时用于防止并发覆盖。 */
        @NotNull @Min(0) Long version
) {
}
