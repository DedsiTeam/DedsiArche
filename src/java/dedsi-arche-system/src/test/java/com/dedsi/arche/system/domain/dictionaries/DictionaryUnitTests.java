package com.dedsi.arche.system.domain.dictionaries;

import com.dedsi.arche.common.exception.BusinessException;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/** 字典聚合纯单元测试，不涉及数据库、Spring 或网络。 */
class DictionaryUnitTests {
    @Test
    void createsEmptyDictionaryWithNormalizedFieldsAndBeijingTimes() {
        var before = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MICROS);
        var dictionary = Dictionary.create(" SYS ", " 系统 ", " Color ", " 颜色 ", " 说明 ", true, null);
        var after = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        assertThat(dictionary.getId()).matches("[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(dictionary.getSystemCode()).isEqualTo("SYS");
        assertThat(dictionary.getSystemName()).isEqualTo("系统");
        assertThat(dictionary.getCode()).isEqualTo("Color");
        assertThat(dictionary.getName()).isEqualTo("颜色");
        assertThat(dictionary.getDescription()).isEqualTo("说明");
        assertThat(dictionary.getCreatedAt()).isBetween(before, after);
        assertThat(dictionary.getUpdatedAt()).isEqualTo(dictionary.getCreatedAt());
        assertThat(dictionary.getVersion()).isNull();
        assertThat(dictionary.getItems()).isEmpty();
    }

    @Test
    void replacesCollectionPreservingRetainedIdsAndSortingByOrderThenCode() {
        var dictionary = create(List.of(item(null, "B", "乙", 2, false, true), item(null, "A", "甲", 1, true, true)));
        var a = dictionary.getItems().getFirst();
        var previousTime = dictionary.getUpdatedAt();
        dictionary.update("颜色", null, true, List.of(
                item(a.getId(), "A", "新甲", 3, false, false), item(null, "C", "丙", 0, true, true)));
        assertThat(dictionary.getItems()).extracting(DictionaryItem::getCode).containsExactly("C", "A");
        var retained = dictionary.getItems().get(1);
        assertThat(retained.getId()).isEqualTo(a.getId());
        assertThat(retained.getDictionaryId()).isEqualTo(dictionary.getId());
        assertThat(retained.getName()).isEqualTo("新甲");
        assertThat(retained.isEnabled()).isFalse();
        assertThat(dictionary.getItems().getFirst().getId()).isNotEqualTo(a.getId());
        assertThat(dictionary.getUpdatedAt()).isAfter(previousTime);
    }

    @Test
    void nullKeepsAndEmptyClearsAndRepeatedConfigurationIsIdempotent() {
        var dictionary = create(List.of(item(null, "A", "甲", 0, false, true)));
        var a = dictionary.getItems().getFirst();
        var time = dictionary.getUpdatedAt();
        dictionary.update("颜色", null, true, null);
        dictionary.update("颜色", null, true, List.of(item(a.getId(), "A", "甲", 0, false, true)));
        dictionary.synchronizeSystemName("SYS", "系统");
        assertThat(dictionary.getUpdatedAt()).isEqualTo(time);
        assertThat(dictionary.getItems()).containsExactly(a);
        dictionary.update("颜色", null, true, List.of());
        assertThat(dictionary.getItems()).isEmpty();
        assertThat(dictionary.getUpdatedAt()).isAfter(time);
        var clearedTime = dictionary.getUpdatedAt();
        dictionary.update("颜色", null, true, List.of());
        assertThat(dictionary.getUpdatedAt()).isEqualTo(clearedTime);
    }

    @Test
    void parentDisablePreservesChildDefaultAndEnabledConfiguration() {
        var dictionary = create(List.of(item(null, "A", "甲", 0, true, true)));
        dictionary.update("颜色", null, false, null);
        assertThat(dictionary.isEnabled()).isFalse();
        assertThat(dictionary.getItems().getFirst().isEnabled()).isTrue();
        assertThat(dictionary.getItems().getFirst().isDefault()).isTrue();
        dictionary.update("颜色", null, true, null);
        assertThat(dictionary.getItems().getFirst().isDefault()).isTrue();
    }

    @Test
    void invalidFinalCollectionCannotPartiallyChangeRootOrExistingItems() {
        var dictionary = create(List.of(item(null, "A", "甲", 0, true, true), item(null, "B", "乙", 1, false, true)));
        var a = dictionary.getItems().getFirst();
        var b = dictionary.getItems().get(1);
        var time = dictionary.getUpdatedAt();
        assertInvalid(() -> dictionary.update("变更", "变更", false, List.of(
                item(a.getId(), "A", "新甲", 0, true, true), item(b.getId(), "B", "新乙", 1, true, true))));
        assertThat(dictionary.getName()).isEqualTo("颜色");
        assertThat(dictionary.getDescription()).isNull();
        assertThat(dictionary.isEnabled()).isTrue();
        assertThat(a.getName()).isEqualTo("甲");
        assertThat(b.isDefault()).isFalse();
        assertThat(dictionary.getUpdatedAt()).isEqualTo(time);
    }

    @Test
    void defaultCanMoveOrBeRemovedButCannotBeDisabledWhileStillDefault() {
        var dictionary = create(List.of(item(null, "A", "甲", 0, true, true), item(null, "B", "乙", 1, false, true)));
        var a = dictionary.getItems().getFirst();
        var b = dictionary.getItems().get(1);
        assertInvalid(() -> dictionary.update("颜色", null, true, List.of(item(a.getId(), "A", "甲", 0, true, false))));
        dictionary.update("颜色", null, true, List.of(
                item(a.getId(), "A", "甲", 0, false, false), item(b.getId(), "B", "乙", 1, true, true)));
        assertThat(a.isDefault()).isFalse();
        assertThat(b.isDefault()).isTrue();
        dictionary.update("颜色", null, true, List.of(item(a.getId(), "A", "甲", 0, false, false)));
        assertThat(dictionary.getItems()).allMatch(value -> !value.isDefault());
    }

    @Test
    void rejectsForeignOrDuplicateIdsChangedCodeAndReplacementWithSameCode() {
        var dictionary = create(List.of(item(null, "A", "甲", 0, false, true)));
        var a = dictionary.getItems().getFirst();
        assertInvalid(() -> dictionary.update("颜色", null, true, List.of(item(UlidCreator.getUlid().toString(), "B", "乙", 0, false, true))));
        assertInvalid(() -> dictionary.update("颜色", null, true, List.of(item(a.getId(), "B", "乙", 0, false, true))));
        assertInvalid(() -> dictionary.update("颜色", null, true, List.of(item(null, "A", "甲", 0, false, true))));
        assertInvalid(() -> dictionary.update("颜色", null, true, List.of(
                item(a.getId(), "A", "甲", 0, false, true), item(a.getId(), "B", "乙", 0, false, true))));
        assertThat(dictionary.getItems()).containsExactly(a);
    }

    @Test
    void rejectsDuplicateNormalizedCodesAndNullItemsButPreservesCaseSensitivity() {
        assertInvalid(() -> create(List.of(item(null, "A", "甲", 0, false, true), item(null, " A ", "乙", 0, false, true))));
        assertInvalid(() -> create(Arrays.asList((DictionaryItemInput) null)));
        assertInvalid(() -> create(List.of(item(null, "A", "甲", -1, false, true))));
        var dictionary = create(List.of(item(null, "a", "同名", 0, false, true), item(null, "A", "同名", 0, false, true)));
        assertThat(dictionary.getItems()).extracting(DictionaryItem::getCode).containsExactly("A", "a");
        assertThatThrownBy(() -> dictionary.getItems().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void validatesTextBoundariesAndRequiredFields() {
        for (String value : Arrays.asList(null, "", " ", "x".repeat(101))) {
            assertInvalid(() -> Dictionary.create(value, "系统", "C", "颜色", null, true, null));
            assertInvalid(() -> Dictionary.create("SYS", value, "C", "颜色", null, true, null));
            assertInvalid(() -> Dictionary.create("SYS", "系统", value, "颜色", null, true, null));
            assertInvalid(() -> Dictionary.create("SYS", "系统", "C", value, null, true, null));
            assertInvalid(() -> create(List.of(item(null, value, "甲", 0, false, true))));
            assertInvalid(() -> create(List.of(item(null, "A", value, 0, false, true))));
        }
        assertInvalid(() -> Dictionary.create("SYS", "系统", "C", "颜色", "x".repeat(501), true, null));
        var dictionary = Dictionary.create("S".repeat(100), "系".repeat(100), "C".repeat(100), "字".repeat(100),
                "说".repeat(500), true, List.of(item(null, "A".repeat(100), "项".repeat(100), Integer.MAX_VALUE, false, true)));
        assertThat(dictionary.getItems()).hasSize(1);
    }

    @Test
    void systemNameSynchronizationOnlyChangesMatchingSystemAndActualNewName() {
        var dictionary = create(List.of());
        var time = dictionary.getUpdatedAt();
        dictionary.synchronizeSystemName("OTHER", "其他");
        assertThat(dictionary.getUpdatedAt()).isEqualTo(time);
        dictionary.synchronizeSystemName("SYS", " 新系统 ");
        assertThat(dictionary.getSystemName()).isEqualTo("新系统");
        assertThat(dictionary.getSystemCode()).isEqualTo("SYS");
        assertThat(dictionary.getCode()).isEqualTo("COLOR");
        assertThat(dictionary.getUpdatedAt()).isAfter(time);
        var changedTime = dictionary.getUpdatedAt();
        dictionary.synchronizeSystemName("SYS", "新系统");
        assertThat(dictionary.getUpdatedAt()).isEqualTo(changedTime);
    }

    /** 构造固定根信息以突出子集合规则。 */
    private static Dictionary create(List<DictionaryItemInput> items) {
        return Dictionary.create("SYS", "系统", "COLOR", "颜色", null, true, items);
    }

    /** 构造一次完整子项输入。 */
    private static DictionaryItemInput item(String id, String code, String name, int sort, boolean isDefault, boolean enabled) {
        return new DictionaryItemInput(id, code, name, sort, isDefault, enabled);
    }

    /** 校验稳定的领域错误码，不依赖具体中文文案。 */
    private static void assertInvalid(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("INVALID_DICTIONARY"));
    }
}
