package com.dedsi.arche.system.domain.menus;

import com.dedsi.arche.common.exception.BusinessException;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.*;

/** 菜单节点纯领域测试；数据库版本、父链和跨聚合事务另由集成验证覆盖。 */
class MenuUnitTests {
    @Test
    void createsThreeDistinctMenuTypesAndNormalizesFields() {
        var before = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MICROS);
        var page = create(MenuType.PAGE, " /users/:id ", " pages/users/detail ", true, true);
        var after = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        assertThat(page.getId()).matches("[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(page.getSystemCode()).isEqualTo("SYS");
        assertThat(page.getCode()).isEqualTo("MenuCode");
        assertThat(page.getRoutePath()).isEqualTo("/users/:id");
        assertThat(page.getComponentPath()).isEqualTo("pages/users/detail");
        assertThat(page.getCreatedAt()).isBetween(before, after);
        assertThat(page.getUpdatedAt()).isEqualTo(page.getCreatedAt());
        assertThat(page.getVersion()).isNull();
        assertThat(page.isCache()).isTrue();
        assertThat(page.isAffix()).isTrue();
        var directory = create(MenuType.DIRECTORY, null, " ", false, false);
        assertThat(directory.getRoutePath()).isNull();
        assertThat(directory.getComponentPath()).isNull();
        var external = create(MenuType.EXTERNAL, "https://example.com/help?q=1#start", null, false, false);
        assertThat(external.getMenuType()).isEqualTo(MenuType.EXTERNAL);
        assertThat(external.getId()).isNotEqualTo(page.getId());
    }

    @Test
    void rejectsConfigurationsThatDoNotBelongToTheirType() {
        assertInvalid(() -> create(null, null, null, false, false));
        assertInvalid(() -> create(MenuType.DIRECTORY, "/route", null, false, false));
        assertInvalid(() -> create(MenuType.DIRECTORY, null, "pages/view", false, false));
        assertInvalid(() -> create(MenuType.DIRECTORY, null, null, true, false));
        assertInvalid(() -> create(MenuType.DIRECTORY, null, null, false, true));
        assertInvalid(() -> create(MenuType.EXTERNAL, "https://example.com", "pages/view", false, false));
        assertInvalid(() -> create(MenuType.EXTERNAL, "https://example.com", null, true, false));
        assertInvalid(() -> create(MenuType.EXTERNAL, "https://example.com", null, false, true));
    }

    @Test
    void rejectsExternalSchemesCredentialsAndMalformedAddresses() {
        for (var route : Arrays.asList(null, "", "/relative", "//example.com", "javascript:alert(1)",
                "data:text/html,test", "file:///tmp/example", "ftp://example.com", "https:///missing",
                "https://user:secret@example.com", "https://user@example.com", "https://example.com:70000", "https://bad host")) {
            assertInvalid(() -> create(MenuType.EXTERNAL, route, null, false, false));
        }
        assertThat(create(MenuType.EXTERNAL, "http://example.com:8080", null, false, false).getRoutePath())
                .isEqualTo("http://example.com:8080");
    }

    @Test
    void validatesInternalRoutesAndRelativeLogicalComponentPaths() {
        for (var route : Arrays.asList(null, "", "relative", "//example.com", "https://example.com",
                "/page?q=1", "/page#fragment", "/a/../b", "/a/./b", "/%2e%2e/b", "/%2fexample.com")) {
            assertInvalid(() -> create(MenuType.PAGE, route, "pages/view", true, false));
        }
        for (var component : Arrays.asList(null, "", "/pages/view", "../view", "pages/../view", "./view",
                "pages//view", "pages/view/", "pages\\view", "pages/view?key=1", "pages/with space")) {
            assertInvalid(() -> create(MenuType.PAGE, "/view", component, true, false));
        }
        assertThat(create(MenuType.PAGE, "/", "pages/Home.tsx", true, false).getComponentPath())
                .isEqualTo("pages/Home.tsx");
    }

    @Test
    void permissionSnapshotMustBeCompleteAndCanBeExplicitlyCleared() {
        var permissionId = UlidCreator.getMonotonicUlid().toString();
        var page = pageWithPermission(permissionId, " read ", " 查看 ");
        assertThat(page.getPermissionCode()).isEqualTo("read");
        assertThat(page.getPermissionName()).isEqualTo("查看");
        assertInvalid(() -> pageWithPermission(null, "read", "查看"));
        assertInvalid(() -> pageWithPermission(permissionId, null, "查看"));
        assertInvalid(() -> pageWithPermission(permissionId, "read", null));
        assertInvalid(() -> pageWithPermission("invalid", "read", "查看"));
        var time = page.getUpdatedAt();
        page.update(null, "菜单", MenuType.PAGE, null, null, null, "/view", "pages/view", 0, null, true, true, true, false);
        assertThat(page.getPermissionId()).isNull();
        assertThat(page.getPermissionCode()).isNull();
        assertThat(page.getPermissionName()).isNull();
        assertThat(page.getUpdatedAt()).isAfter(time);
    }

    @Test
    void invalidLateConfigurationDoesNotPartiallyChangeState() {
        var page = create(MenuType.PAGE, "/view", "pages/view", true, false);
        var time = page.getUpdatedAt();
        var parentId = UlidCreator.getMonotonicUlid().toString();
        assertInvalid(() -> page.update(parentId, "不得保存", MenuType.DIRECTORY,
                null, null, null, null, null, 5, "不得保存", false, false, true, false));
        assertInvalid(() -> page.update(parentId, "不得保存", MenuType.PAGE,
                null, null, null, "/new", "pages/new", -1, null, true, true, true, false));
        assertThat(page.getName()).isEqualTo("菜单");
        assertThat(page.getParentId()).isNull();
        assertThat(page.getDescription()).isNull();
        assertThat(page.getMenuType()).isEqualTo(MenuType.PAGE);
        assertThat(page.getRoutePath()).isEqualTo("/view");
        assertThat(page.isVisible()).isTrue();
        assertThat(page.isEnabled()).isTrue();
        assertThat(page.getUpdatedAt()).isEqualTo(time);
    }

    @Test
    void movingToParentAndBackPreservesImmutableIdentity() {
        var page = create(MenuType.PAGE, "/view", "pages/view", true, false);
        var id = page.getId();
        var created = page.getCreatedAt();
        var parentId = UlidCreator.getMonotonicUlid().toString();
        var time = page.getUpdatedAt();
        updateParent(page, parentId);
        assertThat(page.getParentId()).isEqualTo(parentId);
        assertThat(page.getUpdatedAt()).isAfter(time);
        updateParent(page, null);
        assertThat(page.getParentId()).isNull();
        assertInvalid(() -> updateParent(page, page.getId()));
        assertInvalid(() -> updateParent(page, ""));
        assertThat(page.getId()).isEqualTo(id);
        assertThat(page.getCreatedAt()).isEqualTo(created);
        assertThat(page.getCode()).isEqualTo("MenuCode");
        assertThat(page.getSystemCode()).isEqualTo("SYS");
    }

    @Test
    void identicalConfigurationIsIdempotentAndFlagChangesTouchTheNode() {
        var page = create(MenuType.PAGE, "/view", "pages/view", true, false);
        var time = page.getUpdatedAt();
        updateParent(page, null);
        assertThat(page.getUpdatedAt()).isEqualTo(time);
        page.update(null, "菜单", MenuType.PAGE, null, null, null,
                "/view", "pages/view", 0, null, false, false, false, true);
        assertThat(page.isVisible()).isFalse();
        assertThat(page.isEnabled()).isFalse();
        assertThat(page.isCache()).isFalse();
        assertThat(page.isAffix()).isTrue();
        assertThat(page.getUpdatedAt()).isAfter(time);
    }

    @Test
    void nameEventsOnlyChangeMatchingReferencesAndAreIdempotent() {
        var permissionId = UlidCreator.getMonotonicUlid().toString();
        var page = pageWithPermission(permissionId, "read", "查看");
        var time = page.getUpdatedAt();
        page.synchronizeSystemName("OTHER", "其他");
        page.synchronizePermissionName(UlidCreator.getMonotonicUlid().toString(), "其他");
        assertThat(page.getUpdatedAt()).isEqualTo(time);
        page.synchronizeSystemName("SYS", "新系统");
        assertThat(page.getSystemName()).isEqualTo("新系统");
        assertThat(page.getUpdatedAt()).isAfter(time);
        time = page.getUpdatedAt();
        page.synchronizePermissionName(permissionId, "新权限");
        assertThat(page.getPermissionName()).isEqualTo("新权限");
        assertThat(page.getUpdatedAt()).isAfter(time);
        time = page.getUpdatedAt();
        page.synchronizeSystemName("SYS", "新系统");
        page.synchronizePermissionName(permissionId, "新权限");
        assertThat(page.getUpdatedAt()).isEqualTo(time);
        assertThat(page.getPermissionId()).isEqualTo(permissionId);
        assertThat(page.getPermissionCode()).isEqualTo("read");
    }

    @Test
    void rejectsOversizedFieldsAndAcceptsTheirBoundaries() {
        for (var value : Arrays.asList(null, "", " ", "字".repeat(101))) {
            assertInvalid(() -> Menu.create(value, "系统", null, "C", "菜单", MenuType.DIRECTORY,
                    null, null, null, null, null, 0, null, true, true, false, false));
            assertInvalid(() -> Menu.create("SYS", value, null, "C", "菜单", MenuType.DIRECTORY,
                    null, null, null, null, null, 0, null, true, true, false, false));
            assertInvalid(() -> Menu.create("SYS", "系统", null, value, "菜单", MenuType.DIRECTORY,
                    null, null, null, null, null, 0, null, true, true, false, false));
            assertInvalid(() -> Menu.create("SYS", "系统", null, "C", value, MenuType.DIRECTORY,
                    null, null, null, null, null, 0, null, true, true, false, false));
        }
        assertInvalid(() -> create(MenuType.PAGE, "/" + "字".repeat(500), "pages/view", true, false));
        assertInvalid(() -> create(MenuType.EXTERNAL, "https://example.com/" + "a".repeat(2048), null, false, false));
        assertInvalid(() -> create(MenuType.PAGE, "/view", "a".repeat(256), true, false));
        var menu = create(MenuType.PAGE, "/view", "pages/view", true, false);
        assertInvalid(() -> menu.update(null, "菜单", MenuType.PAGE, null, null, null,
                "/view", "pages/view", 0, "说".repeat(501), true, true, true, false));
        var maximum = "字".repeat(100);
        var boundary = Menu.create(maximum, maximum, null, maximum, maximum, MenuType.PAGE,
                null, null, null, "/" + "字".repeat(499), "a".repeat(255), Integer.MAX_VALUE,
                "说".repeat(500), true, true, true, false);
        assertThat(boundary.getDescription()).hasSize(500);
        assertThat(boundary.getRoutePath()).hasSize(500);
        var externalPrefix = "https://example.com/";
        assertThat(create(MenuType.EXTERNAL, externalPrefix + "a".repeat(2048 - externalPrefix.length()),
                null, false, false).getRoutePath()).hasSize(2048);
    }

    /** 构造指定类型菜单。 */
    private static Menu create(MenuType type, String route, String component, boolean cache, boolean affix) {
        return Menu.create(" SYS ", "系统", null, " MenuCode ", "菜单", type,
                null, null, null, route, component, 0, null, true, true, cache, affix);
    }

    /** 构造有可选权限快照的内页。 */
    private static Menu pageWithPermission(String id, String code, String name) {
        return Menu.create("SYS", "系统", null, "MenuCode", "菜单", MenuType.PAGE,
                id, code, name, "/view", "pages/view", 0, null, true, true, true, false);
    }

    /** 只改变节点的上级引用。 */
    private static void updateParent(Menu menu, String parentId) {
        menu.update(parentId, "菜单", MenuType.PAGE, null, null, null,
                "/view", "pages/view", 0, null, true, true, true, false);
    }

    /** 校验稳定领域错误码。 */
    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("INVALID_MENU"));
    }
}
