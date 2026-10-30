package com.dedsi.arche.system.domain.positions;

import com.dedsi.arche.common.exception.BusinessException;
import com.dedsi.arche.system.domain.permissions.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** 岗位聚合纯单元测试，不加载 Spring、数据库或网络。 */
class PositionUnitTests {
    @Test
    void creationCapturesPermissionSnapshotsAndBeijingTime() {
        var before = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MICROS);
        var permission = permission("system", "read");
        var position = Position.create("system", "系统", " manager ", " 经理 ", " 职责 ", true, 0, List.of(permission));
        assertThat(position.getId()).matches("[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(position.getId()).isNotEqualTo(position().getId());
        assertThat(position.getCode()).isEqualTo("manager");
        assertThat(position.getName()).isEqualTo("经理");
        assertThat(position.getDescription()).isEqualTo("职责");
        assertThat(position.isEnabled()).isTrue();
        assertThat(position.getSortOrder()).isZero();
        assertThat(position.getCreatedAt()).isBetween(before, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        assertThat(position.getUpdatedAt()).isEqualTo(position.getCreatedAt());
        var item = position.getPermissions().getFirst();
        assertThat(item.getId()).matches("[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(item.getId()).isNotEqualTo(position.getId());
        assertThat(item.getPositionId()).isEqualTo(position.getId());
        assertThat(item.getPermissionId()).isEqualTo(permission.getId());
        assertThat(item.getSystemCode()).isEqualTo("system");
        assertThat(item.getSystemName()).isEqualTo("系统");
        assertThat(item.getPermissionCode()).isEqualTo("read");
        assertThat(item.getPermissionName()).isEqualTo("查看");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void requiredFieldsRejectBlankValues(String value) {
        invalid(() -> Position.create("system", "系统", value, "名称", null, true, 0, List.of()), "INVALID_POSITION");
        invalid(() -> Position.create("system", "系统", "code", value, null, true, 0, List.of()), "INVALID_POSITION");
        var position = position();
        invalid(() -> position.update(value, null, true, 0, null), "INVALID_POSITION");
        assertThat(position.getName()).isEqualTo("岗位");
    }

    @Test
    void validatesLengthsAndSortBeforeMutation() {
        var value = "a".repeat(100);
        var position = Position.create("system", "系统", value, value, "文".repeat(500), false, Integer.MAX_VALUE, null);
        assertThat(position.getCode()).hasSize(100);
        assertThat(position.getName()).hasSize(100);
        assertThat(position.getDescription()).hasSize(500);
        invalid(() -> Position.create("system", "系统", value + "a", value, null, true, 0, null), "INVALID_POSITION");
        invalid(() -> Position.create("system", "系统", value, value + "a", null, true, 0, null), "INVALID_POSITION");
        invalid(() -> position.update("新名称", "文".repeat(501), true, 0, List.of()), "INVALID_POSITION");
        invalid(() -> position.update("新名称", null, true, -1, List.of()), "INVALID_POSITION");
        assertThat(position.getName()).isEqualTo(value);
        assertThat(position.isEnabled()).isFalse();
        assertThat(position.getDescription()).hasSize(500);
    }

    @Test
    void replacementPreservesIdsRefreshesSnapshotsAndRemovesOrphans() {
        var first = permission("system", "read");
        var second = permission("other", "read");
        var position = Position.create("system", "系统", "code", "岗位", null, true, 0, List.of(first, second));
        var id = position.getPermissions().getFirst().getId();
        var createdAt = position.getCreatedAt();
        first.update("新系统", "新权限", null, true, 0);
        assertThat(position.getPermissions().getFirst().getSystemName()).isEqualTo("系统");
        position.update("修改", " ", false, 2, List.of(first));
        assertThat(position.getPermissions()).hasSize(1);
        var item = position.getPermissions().getFirst();
        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getSystemName()).isEqualTo("新系统");
        assertThat(item.getPermissionName()).isEqualTo("新权限");
        assertThat(position.getCode()).isEqualTo("code");
        assertThat(position.getCreatedAt()).isEqualTo(createdAt);
        assertThat(position.getDescription()).isNull();
        assertThat(position.isEnabled()).isFalse();
        assertThat(position.getSortOrder()).isEqualTo(2);
        position.update("修改", null, true, 0, null);
        assertThat(position.getPermissions()).hasSize(1);
        position.update("修改", null, true, 0, List.of());
        assertThat(position.getPermissions()).isEmpty();
    }

    @Test
    void invalidPermissionsLeaveAggregateUnchanged() {
        var permission = permission("system", "read");
        var position = Position.create("system", "系统", "code", "岗位", "说明", true, 0, List.of(permission));
        var updatedAt = position.getUpdatedAt();
        invalid(() -> position.update("新名称", null, false, 1, List.of(permission, permission)),
                "DUPLICATE_POSITION_PERMISSION");
        invalid(() -> position.update("新名称", null, false, 1, Arrays.asList(permission, null)),
                "INVALID_POSITION");
        assertThat(position.getName()).isEqualTo("岗位");
        assertThat(position.getDescription()).isEqualTo("说明");
        assertThat(position.isEnabled()).isTrue();
        assertThat(position.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(position.getPermissions()).hasSize(1);
        assertThatThrownBy(() -> position.getPermissions().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void samePermissionCodeAcrossSystemsIsAllowed() {
        var position = Position.create("system", "系统", "code", "岗位", null, true, 0,
                List.of(permission("one", "read"), permission("two", "read")));
        assertThat(position.getPermissions()).hasSize(2);
        assertThat(position.getPermissions().get(0).getId()).isNotEqualTo(position.getPermissions().get(1).getId());
    }

    @Test
    void permissionRenameOnlyChangesMatchingNameAndTouchesRoot() {
        var first = permission("one", "read");
        var other = permission("two", "read");
        var position = Position.create("system", "系统", "code", "岗位", "职责", false, 7, List.of(first, other));
        var item = position.getPermissions().getFirst();
        var itemId = item.getId();
        var createdAt = position.getCreatedAt();
        var before = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MICROS);
        var event = first.update("已改系统名", "新权限名", null, true, 0).orElseThrow();
        position.synchronizePermissionName(event.permissionId(), event.name());
        assertThat(position.getPermissions()).hasSize(2);
        assertThat(item.getPermissionName()).isEqualTo("新权限名");
        assertThat(item.getId()).isEqualTo(itemId);
        assertThat(item.getPositionId()).isEqualTo(position.getId());
        assertThat(item.getPermissionId()).isEqualTo(first.getId());
        assertThat(item.getSystemCode()).isEqualTo("one");
        assertThat(item.getSystemName()).isEqualTo("系统");
        assertThat(item.getPermissionCode()).isEqualTo("read");
        assertThat(position.getPermissions().get(1).getPermissionName()).isEqualTo("查看");
        assertThat(position.getCode()).isEqualTo("code");
        assertThat(position.getName()).isEqualTo("岗位");
        assertThat(position.getDescription()).isEqualTo("职责");
        assertThat(position.isEnabled()).isFalse();
        assertThat(position.getSortOrder()).isEqualTo(7);
        assertThat(position.getCreatedAt()).isEqualTo(createdAt);
        assertThat(position.getUpdatedAt()).isBetween(before,
                LocalDateTime.now(ZoneId.of("Asia/Shanghai")).plusNanos(1000));
        // JPA 版本递增属于集成测试，纯领域方法只负责推进根修改时间。
        assertThat(position.getVersion()).isNull();
    }

    @Test
    void repeatedOrUnrelatedRenameDoesNotTouchAggregate() {
        var permission = permission("system", "read");
        var position = Position.create("system", "系统", "code", "岗位", null, true, 0, List.of(permission));
        position.synchronizePermissionName(permission.getId(), "新名称");
        var updatedAt = position.getUpdatedAt();
        position.synchronizePermissionName(permission.getId(), " 新名称 ");
        position.synchronizePermissionName(permission("other", "read").getId(), "另一个名称");
        assertThat(position.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(position.getPermissions().getFirst().getPermissionName()).isEqualTo("新名称");
    }

    @Test
    void invalidRenameLeavesSnapshotAndRootUnchanged() {
        var permission = permission("system", "read");
        var position = Position.create("system", "系统", "code", "岗位", null, true, 0, List.of(permission));
        var updatedAt = position.getUpdatedAt();
        for (String value : Arrays.asList(null, "", " ", "文".repeat(101))) {
            invalid(() -> position.synchronizePermissionName(permission.getId(), value), "INVALID_POSITION");
        }
        invalid(() -> position.synchronizePermissionName("bad", "名称"), "INVALID_POSITION");
        invalid(() -> position.synchronizePermissionName(null, "名称"), "INVALID_POSITION");
        assertThat(position.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(position.getPermissions().getFirst().getPermissionName()).isEqualTo("查看");
        position.synchronizePermissionName(permission.getId(), "文".repeat(100));
        assertThat(position.getPermissions().getFirst().getPermissionName()).hasSize(100);
    }

    @Test
    void creationRequiresSystemAndBindingCannotChange() {
        for (String value : Arrays.asList(null, "", " ", "文".repeat(101))) {
            invalid(() -> Position.create(value, "系统", "code", "岗位", null, true, 0, List.of()), "INVALID_POSITION");
            invalid(() -> Position.create("system", value, "code", "岗位", null, true, 0, List.of()), "INVALID_POSITION");
        }
        var position = Position.create(" system ", " 系统 ", "code", "岗位", null, true, 0, List.of());
        assertThat(position.getSystemCode()).isEqualTo("system");
        assertThat(position.getSystemName()).isEqualTo("系统");
        invalid(() -> position.update("不得保存", null, true, 0, null, "other", "其他"),
                "POSITION_SYSTEM_IMMUTABLE");
        invalid(() -> position.update("不得保存", null, true, 0, null, " ", "其他"), "INVALID_POSITION");
        assertThat(position.getName()).isEqualTo("岗位");
        assertThat(position.getSystemCode()).isEqualTo("system");
        position.update("岗位", null, true, 0, null, "system", "当前系统名称");
        assertThat(position.getSystemName()).isEqualTo("当前系统名称");
    }

    @Test
    void legacyUnboundPositionSupportsFirstBindingWithoutGuessingSystem() {
        // 模拟 JPA 实例化旧记录后的系统空值；不通过新建工厂放宽新岗位的必填要求。
        var legacy = new Position();
        legacy.update("历史岗位", null, true, 0, List.of(permission("system", "read")));
        legacy.synchronizeSystemName("system", "系统新名");
        assertThat(legacy.getSystemCode()).isNull();
        assertThat(legacy.getSystemName()).isNull();
        assertThat(legacy.getPermissions().getFirst().getSystemName()).isEqualTo("系统新名");
        legacy.update("历史岗位", null, true, 0, null, "system", "系统新名");
        assertThat(legacy.getSystemCode()).isEqualTo("system");
        assertThat(legacy.getSystemName()).isEqualTo("系统新名");
        invalid(() -> legacy.update("历史岗位", null, true, 0, null, "other", "其他"),
                "POSITION_SYSTEM_IMMUTABLE");
    }

    @Test
    void systemRenameUpdatesRootAndMatchingChildrenAsOneAggregate() {
        var first = permission("system", "read");
        var second = permission("system", "write");
        var other = permission("other", "read");
        var position = Position.create("system", "系统", "code", "岗位", "职责", false, 3,
                List.of(first, second, other));
        var ids = position.getPermissions().stream().map(PositionPermission::getId).toList();
        position.synchronizeSystemName("system", "新系统");
        assertThat(position.getSystemName()).isEqualTo("新系统");
        assertThat(position.getPermissions()).extracting(PositionPermission::getSystemName)
                .containsExactly("新系统", "新系统", "系统");
        assertThat(position.getPermissions()).extracting(PositionPermission::getPermissionName)
                .containsOnly("查看");
        assertThat(position.getPermissions()).extracting(PositionPermission::getId).containsExactlyElementsOf(ids);
        assertThat(position.getName()).isEqualTo("岗位");
        assertThat(position.getDescription()).isEqualTo("职责");
        assertThat(position.isEnabled()).isFalse();
        assertThat(position.getSortOrder()).isEqualTo(3);
        var updatedAt = position.getUpdatedAt();
        position.synchronizeSystemName("system", "新系统");
        position.synchronizeSystemName("missing", "无引用");
        assertThat(position.getUpdatedAt()).isEqualTo(updatedAt);
        position.synchronizeSystemName("other", "跨系统新名");
        assertThat(position.getSystemName()).isEqualTo("新系统");
        assertThat(position.getPermissions().get(2).getSystemName()).isEqualTo("跨系统新名");
        invalid(() -> position.synchronizeSystemName("system", " "), "INVALID_POSITION");
        assertThat(position.getSystemName()).isEqualTo("新系统");
    }

    private static Position position() {
        return Position.create("system", "系统", "code", "岗位", null, true, 0, List.of());
    }

    private static Permission permission(String system, String code) {
        return Permission.create(system, "系统", code, "查看", null, true, 0);
    }

    private static void invalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation, String code) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(code));
    }
}
