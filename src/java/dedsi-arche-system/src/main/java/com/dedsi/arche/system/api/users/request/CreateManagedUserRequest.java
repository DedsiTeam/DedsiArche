package com.dedsi.arche.system.api.users.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

/** 管理员创建用户请求；与公开注册请求隔离，岗位分配只允许管理入口。 */
public record CreateManagedUserRequest(
        /* 账号，沿用公开注册的格式和长度，后端统一为小写。 */
        @Schema(description = "账号，3～50 位英文字母、数字或下划线，存储为小写")
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,50}") String username,
        /* 用户姓名，必填且最多 100 字符。 */
        @Schema(description = "姓名，必填，最多 100 字符")
        @NotBlank @Size(max = 100) String displayName,
        /* 明文密码仅用于本次创建，不持久化或返回。 */
        @Schema(description = "密码，12～64 字符且 UTF-8 最多 72 字节", format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 12, max = 64) String password,
        /* 手机号，可选，具体格式由领域行为校验。 */
        @Schema(description = "手机号，可选")
        @Size(max = 16) String phone,
        /* 邮箱，可选，具体格式由领域行为校验。 */
        @Schema(description = "邮箱，可选")
        @Size(max = 254) String email,
        /* 岗位 ID，不允许重复；省略或 null 默认空集合，快照仅由服务端生成。 */
        @Schema(description = "岗位 ID 列表；省略或 null 默认空集合，不能重复；不改变登录或实际权限判定")
        List<@NotNull @Pattern(regexp = "[0-7][0-9A-HJKMNP-TV-Z]{25}") String> positionIds
) {
}
