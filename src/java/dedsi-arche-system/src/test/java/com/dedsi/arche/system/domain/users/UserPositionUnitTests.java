package com.dedsi.arche.system.domain.users;

import com.dedsi.arche.common.exception.BusinessException;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** 用户岗位关联的纯领域测试；不加载 Spring、数据库或网络，不验证 JPA 自动版本行为。 */
class UserPositionUnitTests {
    @Test
    void registrationStartsWithNoAssignments() {
        User user = user();
        assertThat(user.getId()).isInstanceOf(UUID.class);
        assertThat(user.getPositions()).isEmpty();
        assertThat(user.getPositionsRevision()).isZero();
    }

    @Test
    void completeAssignmentStoresSevenFieldsAndKeepsUserIdentity() {
        User user = user();
        UUID userId = user.getId();
        String username = user.getUsername();
        var first = selection("first");
        var second = selection("second");
        user.replacePositions(List.of(first, second));

        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPositionsRevision()).isEqualTo(1);
        assertThat(user.getCredentialVersion()).isZero();
        assertThat(user.getPositions()).hasSize(2);
        UserPosition item = user.getPositions().getFirst();
        assertThat(item.getId()).matches("[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(item.getId()).isNotEqualTo(user.getPositions().get(1).getId());
        assertThat(item.getUserId()).isEqualTo(userId);
        assertThat(item.getPositionId()).isEqualTo(first.positionId());
        assertThat(item.getPositionCode()).isEqualTo(first.positionCode());
        assertThat(item.getPositionName()).isEqualTo(first.positionName());
        assertThat(item.getSystemCode()).isEqualTo(first.systemCode());
        assertThat(item.getSystemName()).isEqualTo(first.systemName());
    }

    @Test
    void replacementRetainsExistingIdentityAndRemovesUnselectedChildren() {
        User user = user();
        var first = selection("first");
        var second = selection("second");
        var third = selection("third");
        user.replacePositions(List.of(first, second));
        String retainedId = user.getPositions().get(1).getId();
        user.replacePositions(List.of(second, third));

        assertThat(user.getPositions()).extracting(UserPosition::getPositionId)
                .containsExactlyInAnyOrder(second.positionId(), third.positionId());
        assertThat(user.getPositions().stream()
                .filter(item -> item.getPositionId().equals(second.positionId())).findFirst().orElseThrow().getId())
                .isEqualTo(retainedId);
        assertThat(user.getPositionsRevision()).isEqualTo(2);
    }

    @Test
    void nullPreservesAndEmptyClearsWithoutRepeatedRevisionChanges() {
        User user = user();
        user.replacePositions(List.of(selection("first")));
        String id = user.getPositions().getFirst().getId();
        user.replacePositions(null);
        assertThat(user.getPositions()).hasSize(1);
        assertThat(user.getPositions().getFirst().getId()).isEqualTo(id);
        assertThat(user.getPositionsRevision()).isEqualTo(1);

        user.replacePositions(List.of());
        assertThat(user.getPositions()).isEmpty();
        assertThat(user.getPositionsRevision()).isEqualTo(2);
        user.replacePositions(List.of());
        assertThat(user.getPositionsRevision()).isEqualTo(2);
    }

    @Test
    void explicitlyResubmittedSnapshotsRefreshAndAdvanceOnlyAssignmentRevision() {
        User user = user();
        var initial = selection("first");
        user.replacePositions(List.of(initial));
        String id = user.getPositions().getFirst().getId();
        var refreshed = new UserPositionSelection(initial.positionId(), "new-code", "新岗位", "new-system", "新系统");
        user.replacePositions(List.of(refreshed));

        var item = user.getPositions().getFirst();
        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getPositionCode()).isEqualTo("new-code");
        assertThat(item.getPositionName()).isEqualTo("新岗位");
        assertThat(item.getSystemCode()).isEqualTo("new-system");
        assertThat(item.getSystemName()).isEqualTo("新系统");
        assertThat(user.getPositionsRevision()).isEqualTo(2);
        assertThat(user.getCredentialVersion()).isZero();
        assertThat(user.getPasswordHash()).isEqualTo("test-hash");
        assertThat(user.isEnabled()).isTrue();

        user.replacePositions(List.of(refreshed));
        assertThat(user.getPositionsRevision()).isEqualTo(2);
    }

    @Test
    void changedInputOrderDoesNotChangeAssociationIdentityOrRevision() {
        User user = user();
        var first = selection("first");
        var second = selection("second");
        user.replacePositions(List.of(first, second));
        var ids = user.getPositions().stream().map(UserPosition::getId).toList();
        user.replacePositions(List.of(second, first));
        assertThat(user.getPositions()).extracting(UserPosition::getId).containsExactlyElementsOf(ids);
        assertThat(user.getPositionsRevision()).isEqualTo(1);
    }

    @Test
    void duplicateOrNullChildrenLeaveEntireAggregateUnchanged() {
        User user = user();
        var original = selection("first");
        var added = selection("second");
        user.replacePositions(List.of(original));
        var changed = new UserPositionSelection(original.positionId(), "changed", "新名称", null, null);
        String id = user.getPositions().getFirst().getId();

        invalid(() -> user.replacePositions(List.of(changed, added, changed)), "DUPLICATE_USER_POSITION");
        invalid(() -> user.replacePositions(Arrays.asList(changed, added, null)), "INVALID_USER_POSITION");
        assertThat(user.getPositions()).hasSize(1);
        assertThat(user.getPositions().getFirst().getId()).isEqualTo(id);
        assertThat(user.getPositions().getFirst().getPositionName()).isEqualTo(original.positionName());
        assertThat(user.getPositionsRevision()).isEqualTo(1);
    }

    @Test
    void legacyUnassignedSystemIsRetainedAndCanBeRefreshedAfterRegistration() {
        User user = user();
        String positionId = newId();
        user.replacePositions(List.of(new UserPositionSelection(positionId, "legacy", "历史岗位", null, null)));
        var item = user.getPositions().getFirst();
        String id = item.getId();
        assertThat(item.getSystemCode()).isNull();
        assertThat(item.getSystemName()).isNull();

        user.replacePositions(List.of(new UserPositionSelection(positionId, "legacy", "历史岗位", "system", "已登记系统")));
        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getSystemCode()).isEqualTo("system");
        assertThat(item.getSystemName()).isEqualTo("已登记系统");
        assertThat(user.getPositionsRevision()).isEqualTo(2);
    }

    @Test
    void assignmentsCannotBeMutatedOutsideAggregate() {
        User user = user();
        user.replacePositions(List.of(selection("first")));
        assertThatThrownBy(() -> user.getPositions().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "bad-id", "0000000000000000000000000i", "80000000000000000000000000"})
    void malformedPositionIdsAreRejected(String value) {
        invalid(() -> new UserPositionSelection(value, "code", "名称", null, null), "INVALID_USER_POSITION");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void requiredSnapshotFieldsRejectBlankValues(String value) {
        invalid(() -> new UserPositionSelection(newId(), value, "名称", null, null), "INVALID_USER_POSITION");
        invalid(() -> new UserPositionSelection(newId(), "code", value, null, null), "INVALID_USER_POSITION");
    }

    @Test
    void snapshotBoundariesAndSystemPairAreValidated() {
        String limit = "文".repeat(100);
        var maximum = new UserPositionSelection(newId(), limit, limit, limit, limit);
        assertThat(maximum.positionCode()).hasSize(100);
        assertThat(maximum.positionName()).hasSize(100);
        assertThat(maximum.systemCode()).hasSize(100);
        assertThat(maximum.systemName()).hasSize(100);
        String oversized = limit + "文";
        invalid(() -> new UserPositionSelection(newId(), oversized, "名称", null, null), "INVALID_USER_POSITION");
        invalid(() -> new UserPositionSelection(newId(), "code", oversized, null, null), "INVALID_USER_POSITION");
        invalid(() -> new UserPositionSelection(newId(), "code", "名称", oversized, "系统"), "INVALID_USER_POSITION");
        invalid(() -> new UserPositionSelection(newId(), "code", "名称", "system", oversized), "INVALID_USER_POSITION");
        invalid(() -> new UserPositionSelection(newId(), "code", "名称", "system", null), "INVALID_USER_POSITION");
        invalid(() -> new UserPositionSelection(newId(), "code", "名称", null, "系统"), "INVALID_USER_POSITION");
        invalid(() -> new UserPositionSelection(newId(), "code", "名称", " ", "系统"), "INVALID_USER_POSITION");
        invalid(() -> new UserPositionSelection(newId(), "code", "名称", "system", " "), "INVALID_USER_POSITION");
        var normalized = new UserPositionSelection(newId(), " code ", " 名称 ", " system ", " 系统 ");
        assertThat(normalized.positionCode()).isEqualTo("code");
        assertThat(normalized.positionName()).isEqualTo("名称");
        assertThat(normalized.systemCode()).isEqualTo("system");
        assertThat(normalized.systemName()).isEqualTo("系统");
    }

    private static User user() {
        return User.register("assignment_user", "岗位测试用户", "test-hash");
    }

    private static UserPositionSelection selection(String code) {
        return new UserPositionSelection(newId(), code, "岗位" + code, "system", "测试系统");
    }

    private static String newId() {
        return UlidCreator.getMonotonicUlid().toString();
    }

    private static void invalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, String code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(code));
    }
}
