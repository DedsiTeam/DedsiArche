package com.dedsi.arche.system.api.positions.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

/** 部分更新岗位；编码不可修改。 */
public record UpdatePositionRequest(
        /* 省略或 null 保持；历史未登记岗位可首次绑定，已绑定只能提交同值，空白拒绝。 */
        @Schema(description = "所属系统编码；null 保持，历史空值允许首次登记，登记后不可更改或解绑。")
        @Size(max = 100) String systemCode,
        /* 岗位名称，省略或 null 保持，空白拒绝。 */
        @Schema(description = "岗位名称，省略或 null 保持，空白拒绝。")
        @Size(max = 100) String name,
        /* 岗位说明，省略或 null 保持，空白清除。 */
        @Schema(description = "岗位说明，省略或 null 保持，空白清除。")
        @Size(max = 500) String description,
        /* 是否启用，省略或 null 保持。 */
        @Schema(description = "是否启用，省略或 null 保持。")
        Boolean enabled,
        /* 排序值，省略或 null 保持。 */
        @Schema(description = "排序值，省略或 null 保持。")
        @Min(0) Integer sortOrder,
        /* 权限 ID 集合；省略或 null 保持，空数组清空，非空全量替换并刷新快照。 */
        @Schema(description = "权限 ID 集合；省略或 null 保持，空数组清空，非空全量替换并刷新快照。")
        List<@NotNull @Pattern(regexp = "[0-7][0-9A-HJKMNP-TV-Z]{25}") String> permissionIds,
        /* 当前岗位版本，必须提交。 */
        @Schema(description = "当前岗位版本，必须提交。")
        @NotNull @Min(0) Long version
) {
}
