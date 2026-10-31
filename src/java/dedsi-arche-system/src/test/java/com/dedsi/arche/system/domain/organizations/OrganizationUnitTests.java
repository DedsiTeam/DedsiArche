package com.dedsi.arche.system.domain.organizations;

import com.dedsi.arche.common.exception.BusinessException;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.*;

/** 组织节点自身行为的纯单元测试；父链、数据库版本及事务规则由集成验证覆盖。 */
class OrganizationUnitTests {
    @Test
    void createsNormalizedIndependentNodeWithBeijingTimestamps() {
        var before = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MICROS);
        var organization = Organization.create(" SYS ", " 系统 ", null, " HeadOffice ", " 总部 ",
                " Alias ", " 简称 ", " ", null, 0, " 说明 ", true);
        var after = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        assertThat(organization.getId()).matches("[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(organization.getSystemCode()).isEqualTo("SYS");
        assertThat(organization.getSystemName()).isEqualTo("系统");
        assertThat(organization.getParentId()).isNull();
        assertThat(organization.getCode()).isEqualTo("HeadOffice");
        assertThat(organization.getName()).isEqualTo("总部");
        assertThat(organization.getName1()).isEqualTo("Alias");
        assertThat(organization.getName2()).isEqualTo("简称");
        assertThat(organization.getName3()).isNull();
        assertThat(organization.getName4()).isNull();
        assertThat(organization.getDescription()).isEqualTo("说明");
        assertThat(organization.getCreatedAt()).isBetween(before, after);
        assertThat(organization.getUpdatedAt()).isEqualTo(organization.getCreatedAt());
        assertThat(organization.getVersion()).isNull();
    }

    @Test
    void movingNodeAndClearingParentAdvanceModificationTimeWithoutChangingIdentity() {
        var organization = create();
        var id = organization.getId();
        var created = organization.getCreatedAt();
        var time = organization.getUpdatedAt();
        var parentId = UlidCreator.getUlid().toString();
        organization.update(parentId, "总部", null, null, null, null, 0, null, true);
        assertThat(organization.getParentId()).isEqualTo(parentId);
        assertThat(organization.getUpdatedAt()).isAfter(time);
        time = organization.getUpdatedAt();
        organization.update(null, "总部", null, null, null, null, 0, null, true);
        assertThat(organization.getParentId()).isNull();
        assertThat(organization.getUpdatedAt()).isAfter(time);
        assertThat(organization.getId()).isEqualTo(id);
        assertThat(organization.getCreatedAt()).isEqualTo(created);
        assertThat(organization.getCode()).isEqualTo("HEAD");
        assertThat(organization.getSystemCode()).isEqualTo("SYS");
    }

    @Test
    void identicalNormalizedInputDoesNotTouchNodeAndOptionalTextCanBeCleared() {
        var organization = create();
        var time = organization.getUpdatedAt();
        organization.update(null, " 总部 ", " ", "", null, "\t", 0, " ", true);
        assertThat(organization.getUpdatedAt()).isEqualTo(time);
        organization.update(null, "总部", "一", "二", "三", "四", 0, "说明", true);
        assertThat(organization.getUpdatedAt()).isAfter(time);
        time = organization.getUpdatedAt();
        organization.update(null, "总部", null, " ", "", null, 0, null, true);
        assertThat(organization.getName1()).isNull();
        assertThat(organization.getName2()).isNull();
        assertThat(organization.getName3()).isNull();
        assertThat(organization.getName4()).isNull();
        assertThat(organization.getDescription()).isNull();
        assertThat(organization.getUpdatedAt()).isAfter(time);
    }

    @Test
    void standaloneEnableAndSortChangesTouchOnlyCurrentNode() {
        var organization = create();
        var time = organization.getUpdatedAt();
        organization.update(null, "总部", null, null, null, null, 0, null, false);
        assertThat(organization.isEnabled()).isFalse();
        assertThat(organization.getUpdatedAt()).isAfter(time);
        time = organization.getUpdatedAt();
        organization.update(null, "总部", null, null, null, null, 8, null, false);
        assertThat(organization.getSortOrder()).isEqualTo(8);
        assertThat(organization.getUpdatedAt()).isAfter(time);
        organization.update(null, "总部", null, null, null, null, 8, null, true);
        assertThat(organization.isEnabled()).isTrue();
    }

    @Test
    void invalidLateFieldCannotPartiallyChangeAnyNodeState() {
        var organization = create();
        var time = organization.getUpdatedAt();
        var parentId = UlidCreator.getUlid().toString();
        assertInvalid(() -> organization.update(parentId, "不得保存", "一", "二", "三", "四", -1, "说明", false));
        assertInvalid(() -> organization.update(parentId, "不得保存", "一", "二", "三", "四", 1, "长".repeat(501), false));
        assertThat(organization.getParentId()).isNull();
        assertThat(organization.getName()).isEqualTo("总部");
        assertThat(organization.getName1()).isNull();
        assertThat(organization.getDescription()).isNull();
        assertThat(organization.isEnabled()).isTrue();
        assertThat(organization.getSortOrder()).isZero();
        assertThat(organization.getUpdatedAt()).isEqualTo(time);
    }

    @Test
    void rejectsInvalidParentIdentifiersAndSelfParentBeforeMutation() {
        var organization = create();
        var time = organization.getUpdatedAt();
        for (String parentId : Arrays.asList("", " ", "not-ulid", "Z".repeat(26), organization.getId())) {
            assertInvalid(() -> organization.update(parentId, "不得保存", null, null, null, null, 0, null, true));
        }
        assertThat(organization.getParentId()).isNull();
        assertThat(organization.getName()).isEqualTo("总部");
        assertThat(organization.getUpdatedAt()).isEqualTo(time);
    }

    @Test
    void validatesRequiredTextAndEachOptionalLengthBoundary() {
        for (String value : Arrays.asList(null, "", " ", "字".repeat(101))) {
            assertInvalid(() -> Organization.create(value, "系统", null, "HEAD", "总部", null, null, null, null, 0, null, true));
            assertInvalid(() -> Organization.create("SYS", value, null, "HEAD", "总部", null, null, null, null, 0, null, true));
            assertInvalid(() -> Organization.create("SYS", "系统", null, value, "总部", null, null, null, null, 0, null, true));
            assertInvalid(() -> Organization.create("SYS", "系统", null, "HEAD", value, null, null, null, null, 0, null, true));
        }
        var organization = create();
        var tooLong = "字".repeat(101);
        assertInvalid(() -> organization.update(null, "总部", tooLong, null, null, null, 0, null, true));
        assertInvalid(() -> organization.update(null, "总部", null, tooLong, null, null, 0, null, true));
        assertInvalid(() -> organization.update(null, "总部", null, null, tooLong, null, 0, null, true));
        assertInvalid(() -> organization.update(null, "总部", null, null, null, tooLong, 0, null, true));
        var maximum = "字".repeat(100);
        var maximumOrganization = Organization.create(maximum, maximum, null, maximum, maximum,
                maximum, maximum, maximum, maximum, Integer.MAX_VALUE, "说".repeat(500), false);
        assertThat(maximumOrganization.getName4()).hasSize(100);
        assertThat(maximumOrganization.getDescription()).hasSize(500);
        assertThat(maximumOrganization.getSortOrder()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void systemNameEventIsScopedAndIdempotentWithoutChangingOtherFields() {
        var organization = create();
        var time = organization.getUpdatedAt();
        organization.synchronizeSystemName("OTHER", "其他系统");
        organization.synchronizeSystemName("SYS", "系统");
        assertThat(organization.getUpdatedAt()).isEqualTo(time);
        organization.synchronizeSystemName("SYS", " 新系统 ");
        assertThat(organization.getSystemName()).isEqualTo("新系统");
        assertThat(organization.getUpdatedAt()).isAfter(time);
        time = organization.getUpdatedAt();
        organization.synchronizeSystemName("SYS", "新系统");
        assertThat(organization.getUpdatedAt()).isEqualTo(time);
        assertThat(organization.getSystemCode()).isEqualTo("SYS");
        assertThat(organization.getCode()).isEqualTo("HEAD");
        assertThat(organization.getName()).isEqualTo("总部");
        assertInvalid(() -> organization.synchronizeSystemName("SYS", " "));
        assertThat(organization.getSystemName()).isEqualTo("新系统");
    }

    /** 构造无上级、无可选名称的基础节点。 */
    private static Organization create() {
        return Organization.create("SYS", "系统", null, "HEAD", "总部", null, null, null, null, 0, null, true);
    }

    /** 校验统一领域错误，不依赖具体中文文案。 */
    private static void assertInvalid(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("INVALID_ORGANIZATION"));
    }
}
