package com.dedsi.arche.system.domain.permissions;

import com.dedsi.arche.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

/** 验证权限聚合的创建、修改和领域约束，不依赖 Spring、数据库或网络。 */
class PermissionUnitTests {

    @Test
    @DisplayName("创建权限生成 ULID，规范化文本并初始化相同审计时间")
    void creationInitializesPermission() {
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        Permission permission = Permission.create(
                " arche ", " 系统 ", " user:read ", " 查看用户 ", " 说明 ", true, 0
        );

        assertThat(permission.getId()).matches("[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(permission.getId()).isNotEqualTo(newPermission().getId());
        assertThat(permission.getSystemCode()).isEqualTo("arche");
        assertThat(permission.getSystemName()).isEqualTo("系统");
        assertThat(permission.getCode()).isEqualTo("user:read");
        assertThat(permission.getName()).isEqualTo("查看用户");
        assertThat(permission.getDescription()).isEqualTo("说明");
        assertThat(permission.isEnabled()).isTrue();
        assertThat(permission.getSortOrder()).isZero();
        assertThat(permission.getCreatedAt()).isBetween(before, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        assertThat(permission.getUpdatedAt()).isEqualTo(permission.getCreatedAt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    @DisplayName("四个必填文本均拒绝空值与空白")
    void requiredFieldsRejectBlankValues(String value) {
        assertInvalid(() -> Permission.create(value, "系统", "read", "查看", null, true, 0));
        assertInvalid(() -> Permission.create("arche", value, "read", "查看", null, true, 0));
        assertInvalid(() -> Permission.create("arche", "系统", value, "查看", null, true, 0));
        assertInvalid(() -> Permission.create("arche", "系统", "read", value, null, true, 0));
    }

    @Test
    @DisplayName("必填文本接受 100 字符，说明接受 500 字符")
    void maximumLengthsAreAccepted() {
        String text = "a".repeat(100);
        Permission permission = Permission.create(text, text, text, text, "文".repeat(500), false, 0);

        assertThat(permission.getSystemCode()).isEqualTo(text);
        assertThat(permission.getSystemName()).isEqualTo(text);
        assertThat(permission.getCode()).isEqualTo(text);
        assertThat(permission.getName()).isEqualTo(text);
        assertThat(permission.getDescription()).hasSize(500);
        assertThat(permission.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("四个必填文本超过 100 字符均被拒绝")
    void oversizedRequiredFieldsAreRejected() {
        String value = "a".repeat(101);
        assertInvalid(() -> Permission.create(value, "系统", "read", "查看", null, true, 0));
        assertInvalid(() -> Permission.create("arche", value, "read", "查看", null, true, 0));
        assertInvalid(() -> Permission.create("arche", "系统", value, "查看", null, true, 0));
        assertInvalid(() -> Permission.create("arche", "系统", "read", value, null, true, 0));
    }

    @Test
    @DisplayName("创建和更新都拒绝超长说明与负排序值")
    void invalidDescriptionAndSortOrderAreRejected() {
        assertInvalid(() -> Permission.create("arche", "系统", "read", "查看", "文".repeat(501), true, 0));
        assertInvalid(() -> Permission.create("arche", "系统", "read", "查看", null, true, -1));
        assertInvalid(() -> newPermission().update("系统", "查看", "文".repeat(501), true, 0));
        assertInvalid(() -> newPermission().update("系统", "查看", null, true, -1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("更新时也拒绝空系统名称和权限名称")
    void updatesValidateRequiredNames(String value) {
        assertInvalid(() -> newPermission().update(value, "查看", null, true, 0));
        assertInvalid(() -> newPermission().update("系统", value, null, true, 0));
    }

    @Test
    @DisplayName("更新拒绝超过 100 字符的系统名称和权限名称")
    void updatesRejectOversizedNames() {
        String value = "文".repeat(101);
        assertInvalid(() -> newPermission().update(value, "查看", null, true, 0));
        assertInvalid(() -> newPermission().update("系统", value, null, true, 0));
    }

    @Test
    @DisplayName("更新可编辑字段并保留主键、系统编码、权限编码及创建时间")
    void updatePreservesImmutableFields() {
        Permission permission = newPermission();
        String id = permission.getId();
        LocalDateTime createdAt = permission.getCreatedAt();
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));

        permission.update(" 新系统 ", " 新名称 ", " 新说明 ", false, 7);

        assertThat(permission.getSystemName()).isEqualTo("新系统");
        assertThat(permission.getName()).isEqualTo("新名称");
        assertThat(permission.getDescription()).isEqualTo("新说明");
        assertThat(permission.isEnabled()).isFalse();
        assertThat(permission.getSortOrder()).isEqualTo(7);
        assertThat(permission.getId()).isEqualTo(id);
        assertThat(permission.getSystemCode()).isEqualTo("arche");
        assertThat(permission.getCode()).isEqualTo("user:read");
        assertThat(permission.getCreatedAt()).isEqualTo(createdAt);
        assertThat(permission.getUpdatedAt()).isBetween(before, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));

        permission.update("新系统", "新名称", null, true, 0);
        assertThat(permission.isEnabled()).isTrue();
        assertThat(permission.getSortOrder()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("空说明在创建时存为空值，在更新时清除原说明")
    void emptyDescriptionIsCleared(String description) {
        Permission permission = newPermission();
        permission.update("系统", "查看", description, true, 0);
        assertThat(permission.getDescription()).isNull();

        Permission created = Permission.create("arche", "系统", "read", "查看", description, true, 0);
        assertThat(created.getDescription()).isNull();
    }

    @Test
    @DisplayName("仅名称实际改变时产生包含前后名称的纯领域事件")
    void actualRenameProducesDomainEvent() {
        Permission permission = newPermission();
        assertThat(permission.update("系统", " 查看 ", "说明变化", false, 2)).isEmpty();
        var event = permission.update("系统", " 新名称 ", "说明", true, 3).orElseThrow();
        assertThat(event.permissionId()).isEqualTo(permission.getId());
        assertThat(event.previousName()).isEqualTo("查看");
        assertThat(event.name()).isEqualTo("新名称");
        assertThat(permission.getName()).isEqualTo(event.name());
        assertThat(permission.update("另一系统名称", "新名称", null, false, 0)).isEmpty();
        assertThat(event.name()).isEqualTo("新名称");
    }

    @Test
    @DisplayName("失败更新不修改名称或其他字段，后续事件仍使用原始名称")
    void failedUpdateDoesNotMutateOrProduceRename() {
        Permission permission = newPermission();
        LocalDateTime updatedAt = permission.getUpdatedAt();
        assertInvalid(() -> permission.update("新系统", "新名称", "文".repeat(501), false, 1));
        assertInvalid(() -> permission.update("新系统", "新名称", null, false, -1));
        assertThat(permission.getSystemName()).isEqualTo("系统");
        assertThat(permission.getName()).isEqualTo("查看");
        assertThat(permission.getDescription()).isEqualTo("原说明");
        assertThat(permission.isEnabled()).isTrue();
        assertThat(permission.getSortOrder()).isZero();
        assertThat(permission.getUpdatedAt()).isEqualTo(updatedAt);
        var event = permission.update("系统", "成功更名", null, true, 0).orElseThrow();
        assertThat(event.previousName()).isEqualTo("查看");
    }

    @Test
    @DisplayName("系统名称同步只改系统名称且重复处理幂等，不影响权限名称")
    void systemNameSynchronizationIsIdempotent() {
        var permission = newPermission();
        var createdAt = permission.getCreatedAt();
        permission.synchronizeSystemName(" 新系统 ");
        assertThat(permission.getSystemName()).isEqualTo("新系统");
        assertThat(permission.getSystemCode()).isEqualTo("arche");
        assertThat(permission.getName()).isEqualTo("查看");
        assertThat(permission.getCode()).isEqualTo("user:read");
        assertThat(permission.getDescription()).isEqualTo("原说明");
        assertThat(permission.getCreatedAt()).isEqualTo(createdAt);
        var updatedAt = permission.getUpdatedAt();
        permission.synchronizeSystemName("新系统");
        assertThat(permission.getUpdatedAt()).isEqualTo(updatedAt);
        assertInvalid(() -> permission.synchronizeSystemName(" "));
        assertInvalid(() -> permission.synchronizeSystemName("文".repeat(101)));
        assertThat(permission.getSystemName()).isEqualTo("新系统");
        assertThat(permission.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(permission.update("新系统", "查看", "原说明", true, 0)).isEmpty();
    }

    /** 仅在内存中创建测试权限，不写入任何数据库。 */
    private Permission newPermission() {
        return Permission.create("arche", "系统", "user:read", "查看", "原说明", true, 0);
    }

    /** 断言领域校验的稳定错误码。 */
    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_PERMISSION"));
    }
}
