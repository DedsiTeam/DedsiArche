package com.dedsi.arche.system.domain.systems.query;

import io.swagger.v3.oas.annotations.media.Schema;

/** 系统下拉选项，仅包含选择值与展示名称。 */
public record ManagedSystemOptionDto(
        /* 系统编码，作为下拉选项值及权限所属系统参数。 */
        @Schema(description = "系统编码，下拉选项值。")
        String code,
        /* 系统名称，下拉框展示文本。 */
        @Schema(description = "系统名称，下拉展示文本。")
        String name
) {
}
