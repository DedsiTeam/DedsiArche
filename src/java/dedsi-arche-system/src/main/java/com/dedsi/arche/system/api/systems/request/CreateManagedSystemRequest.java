package com.dedsi.arche.system.api.systems.request;

import com.dedsi.arche.system.domain.systems.SystemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 系统管理请求，字段校验与领域业务约束共同生效。 */
public record CreateManagedSystemRequest(
        /* 系统编码，必填且创建后不可修改。 */
        @Schema(description = "系统编码，必填且创建后不可修改。")
        @NotBlank @Size(max = 100)
        String code,
        /* 系统名称。 */
        @Schema(description = "系统名称。")
        @NotBlank @Size(max = 100)
        String name,
        /* 系统说明，可为空。 */
        @Schema(description = "系统说明，可为空。")
        @Size(max = 512)
        String description,
        /* HTTP/HTTPS 入口，正常状态必填。 */
        @Schema(description = "HTTP/HTTPS 入口，正常状态必填。")
        @Size(max = 2048)
        String entryUrl,
        /* 状态，省略时默认 NOT_LAUNCHED。 */
        @Schema(description = "状态，省略时默认 NOT_LAUNCHED。")
        SystemStatus status,
        /* 排序值，省略时默认 0。 */
        @Schema(description = "排序值，省略时默认 0。")
        @Min(0)
        Integer sortOrder
) {
}
