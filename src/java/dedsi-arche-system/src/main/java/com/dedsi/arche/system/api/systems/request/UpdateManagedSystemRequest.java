package com.dedsi.arche.system.api.systems.request;

import com.dedsi.arche.system.domain.systems.SystemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** 系统管理请求，字段校验与领域业务约束共同生效。 */
public record UpdateManagedSystemRequest(
        /* 新名称；null 或省略时保留原值。 */
        @Schema(description = "新名称；null 或省略时保留原值。")
        @Size(max = 100)
        String name,
        /* 说明；null 保留，空串清除。 */
        @Schema(description = "说明；null 保留，空串清除。")
        @Size(max = 512)
        String description,
        /* 入口；null 保留，空串清除，正常状态不可清除。 */
        @Schema(description = "入口；null 保留，空串清除，正常状态不可清除。")
        @Size(max = 2048)
        String entryUrl,
        /* 状态；null 保留。 */
        @Schema(description = "状态；null 保留。")
        SystemStatus status,
        /* 排序；null 保留。 */
        @Schema(description = "排序；null 保留。")
        @Min(0)
        Integer sortOrder,
        /* 当前版本，必填。 */
        @Schema(description = "当前版本，必填。")
        @NotNull @Min(0)
        Long version
) {
}
