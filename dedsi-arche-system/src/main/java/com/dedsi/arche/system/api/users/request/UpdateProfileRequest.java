package com.dedsi.arche.system.api.users.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

/**
 * 当前用户资料修改请求。
 *
 * @param displayName 向调用方展示的用户名称。
 */
public record UpdateProfileRequest(
        @Schema(description = "新的显示名称", example = "Alice Chen")
        @NotBlank
        @Size(max = 100)
        String displayName
) {
}
