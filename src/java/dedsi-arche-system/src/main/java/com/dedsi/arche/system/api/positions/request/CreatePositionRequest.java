package com.dedsi.arche.system.api.positions.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

/** 新增岗位请求；系统和权限快照仅由服务端读取。 */
public record CreatePositionRequest(
        /* 所属系统编码，必须登记且最多 100 字符；名称由服务端读取。 */
        @Schema(description = "所属系统编码，必填；systemName 由服务端读取。")
        @NotBlank @Size(max = 100) String systemCode,
        /* 岗位编码，创建后不可变，区分大小写。 */
        @Schema(description = "岗位编码，创建后不可变，区分大小写。")
        @NotBlank @Size(max = 100) String code,
        /* 岗位名称，最多 100 字符。 */
        @Schema(description = "岗位名称，最多 100 字符。")
        @NotBlank @Size(max = 100) String name,
        /* 岗位说明，可空。 */
        @Schema(description = "岗位说明，可空。")
        @Size(max = 500) String description,
        /* 是否启用，省略或 null 默认 true。 */
        @Schema(description = "是否启用，省略或 null 默认 true。")
        Boolean enabled,
        /* 排序值，省略或 null 默认 0。 */
        @Schema(description = "排序值，省略或 null 默认 0。")
        @Min(0) Integer sortOrder,
        /* 权限 ID 集合，不重复；省略或 null 默认空集合。 */
        @Schema(description = "权限 ID 集合，不重复；省略或 null 默认空集合。")
        List<@NotNull @Pattern(regexp = "[0-7][0-9A-HJKMNP-TV-Z]{25}") String> permissionIds
) {
}
