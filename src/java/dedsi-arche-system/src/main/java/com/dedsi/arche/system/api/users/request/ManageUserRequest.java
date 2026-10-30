package com.dedsi.arche.system.api.users.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

/** 管理用户资料与岗位集合，保留原有姓名、启用状态及版本必填契约。 */
public record ManageUserRequest(
        /* 用户姓名，必填。 */
        @Schema(description = "姓名，必填且最多 100 字符")
        @NotBlank @Size(max = 100) String displayName,
        /* 启用状态，必填，不能停用当前登录用户。 */
        @Schema(description = "启用状态，必填")
        @NotNull Boolean enabled,
        /* 用户聚合版本，保护资料和岗位子实体的并发修改。 */
        @Schema(description = "当前用户版本，必填")
        @NotNull @PositiveOrZero Long version,
        /* 省略或 null 保持，空串清除。 */
        @Schema(description = "手机号，null 保持，空串清除")
        @Size(max = 16) String phone,
        /* 省略或 null 保持，空串清除。 */
        @Schema(description = "邮箱，null 保持，空串清除")
        @Size(max = 254) String email,
        /* null 保持，空数组清空，非空全量替换并刷新快照；保留未移除关联的 ID。 */
        @Schema(description = "岗位 ID 列表；省略或 null 保持，空数组清空，非空全量替换并刷新快照")
        List<@NotNull @Pattern(regexp = "[0-7][0-9A-HJKMNP-TV-Z]{25}") String> positionIds
) {
    /** 保留既有 Java 调用的三参数构造。 */
    public ManageUserRequest(String displayName, Boolean enabled, Long version) {
        this(displayName, enabled, version, null, null, null);
    }

    /** 保留既有 Java 调用的五参数构造。 */
    public ManageUserRequest(String displayName, Boolean enabled, Long version, String phone, String email) {
        this(displayName, enabled, version, phone, email, null);
    }
}
