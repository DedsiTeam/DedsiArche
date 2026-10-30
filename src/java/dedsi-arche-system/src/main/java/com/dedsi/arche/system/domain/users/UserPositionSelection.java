package com.dedsi.arche.system.domain.users;

import com.dedsi.arche.common.exception.BusinessException;

/** 服务端从岗位定义读取的不可变关联输入；不由客户端提供展示快照。 */
public record UserPositionSelection(
        /* 被引用岗位的 26 位 ULID。 */
        String positionId,
        /* 配置时的岗位编码，必填。 */
        String positionCode,
        /* 配置时的岗位名称，必填。 */
        String positionName,
        /* 配置时岗位根的系统编码；历史未登记岗位允许为空。 */
        String systemCode,
        /* 配置时岗位根的系统名称；与系统编码同时为空或同时有值。 */
        String systemName
) {
    /** 校验并规范化完整快照，构造失败不会影响用户聚合。 */
    public UserPositionSelection {
        if (positionId == null || !positionId.matches("[0-7][0-9A-HJKMNP-TV-Z]{25}")) {
            throw invalid("岗位 ID 必须是 26 位大写 ULID");
        }
        positionCode = required(positionCode, "岗位编码");
        positionName = required(positionName, "岗位名称");
        if ((systemCode == null) != (systemName == null)) {
            throw invalid("岗位所属系统编码和名称必须同时为空或同时填写");
        }
        if (systemCode != null) {
            systemCode = required(systemCode, "系统编码");
            systemName = required(systemName, "系统名称");
        }
    }

    /** 必填展示字段最长 100 字符，保存前移除首尾空白。 */
    private static String required(String value, String label) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw invalid(label + "不能为空且最多 100 字符");
        }
        return value.trim();
    }

    /** 关联输入采用稳定业务错误码。 */
    private static BusinessException invalid(String message) {
        return new BusinessException("INVALID_USER_POSITION", message);
    }
}
