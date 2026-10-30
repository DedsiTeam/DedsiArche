package com.dedsi.arche.system.domain.systems;

import com.dedsi.arche.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import static org.assertj.core.api.Assertions.*;

/** 系统聚合纯单元测试，不加载 Spring、不访问数据库或网络。 */
class ManagedSystemUnitTests {

    @Test
    @DisplayName("创建系统生成 ULID，规范化文本并使用北京时间")
    void createInitializesSystem() {
        var before = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        var system = ManagedSystem.create(
                " arche ", " 平台 ", " 说明 ", null, SystemStatus.NOT_LAUNCHED, 0
        );
        assertThat(system.getId()).matches("[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(system.getId()).isNotEqualTo(newSystem().getId());
        assertThat(system.getCode()).isEqualTo("arche");
        assertThat(system.getName()).isEqualTo("平台");
        assertThat(system.getDescription()).isEqualTo("说明");
        assertThat(system.getEntryUrl()).isNull();
        assertThat(system.getStatus()).isEqualTo(SystemStatus.NOT_LAUNCHED);
        assertThat(system.getSortOrder()).isZero();
        assertThat(system.getCreatedAt()).isBetween(before, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        assertThat(system.getUpdatedAt()).isEqualTo(system.getCreatedAt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("系统编码和名称不能为空")
    void blankRequiredTextIsRejected(String value) {
        assertError(() -> ManagedSystem.create(value, "系统", null, null, SystemStatus.NOT_LAUNCHED, 0),
                "INVALID_SYSTEM_TEXT");
        assertError(() -> ManagedSystem.create("arche", value, null, null, SystemStatus.NOT_LAUNCHED, 0),
                "INVALID_SYSTEM_TEXT");
    }

    @Test
    @DisplayName("接受长度上界并拒绝超长编码、名称、说明及地址")
    void textLengthsAreBounded() {
        String maximumUrl = "https://example.com/" + "a".repeat(2028);
        var system = ManagedSystem.create(
                "a".repeat(100), "名".repeat(100), "文".repeat(512),
                maximumUrl, SystemStatus.NORMAL, 0
        );
        assertThat(system.getEntryUrl()).hasSize(2048);
        assertThat(system.getDescription()).hasSize(512);
        assertError(() -> ManagedSystem.create("a".repeat(101), "系统", null, null,
                SystemStatus.NOT_LAUNCHED, 0), "INVALID_SYSTEM_TEXT");
        assertError(() -> newSystem().update("名".repeat(101), null, null,
                SystemStatus.NOT_LAUNCHED, 0), "INVALID_SYSTEM_TEXT");
        assertError(() -> newSystem().update("系统", "文".repeat(513), null,
                SystemStatus.NOT_LAUNCHED, 0), "INVALID_SYSTEM_TEXT");
        assertError(() -> newSystem().update("系统", null, maximumUrl + "a",
                SystemStatus.NORMAL, 0), "INVALID_SYSTEM_TEXT");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/", "http://localhost:8080/app", "HTTPS://example.com",
            "https://example.com/app?source=portal#home", "http://[::1]:8080/"
    })
    @DisplayName("支持 HTTP/HTTPS 绝对地址和尾斜杠")
    void validUrlsAreAccepted(String url) {
        var system = newSystem();
        system.update("系统", null, " " + url + " ", SystemStatus.NORMAL, 0);
        assertThat(system.getEntryUrl()).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com", "javascript:alert(1)", "//example.com", "/relative",
            "https://", "https://user:password@example.com", "https://example.com/a b",
            "http://example.com:70000", "http://example.com:0"
    })
    @DisplayName("拒绝非 HTTP 协议、相对地址、账号密码和非法网址")
    void invalidUrlsAreRejected(String url) {
        assertError(() -> newSystem().update("系统", null, url, SystemStatus.NORMAL, 0),
                "INVALID_SYSTEM_ENTRY_URL");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("正常状态不允许缺少入口")
    void normalStatusRequiresEntry(String url) {
        assertError(() -> ManagedSystem.create("arche", "系统", null, url, SystemStatus.NORMAL, 0),
                "SYSTEM_ENTRY_REQUIRED");
        assertError(() -> newSystem().update("系统", null, url, SystemStatus.NORMAL, 0),
                "SYSTEM_ENTRY_REQUIRED");
    }

    @ParameterizedTest
    @EnumSource(value = SystemStatus.class, names = "NORMAL", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("非正常业务状态允许尚未配置入口")
    void otherStatusesAllowMissingEntry(SystemStatus status) {
        var system = newSystem();
        system.update("系统", null, null, status, 0);
        assertThat(system.getStatus()).isEqualTo(status);
        assertThat(system.getEntryUrl()).isNull();
    }

    @Test
    @DisplayName("成功更新保留标识和编码，修改状态并刷新更新时间")
    void updatePreservesIdentity() {
        var system = newSystem();
        var id = system.getId();
        var createdAt = system.getCreatedAt();
        var before = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        system.update(" 新系统 ", " 新说明 ", "https://example.com/", SystemStatus.NORMAL, 8);
        assertThat(system.getName()).isEqualTo("新系统");
        assertThat(system.getDescription()).isEqualTo("新说明");
        assertThat(system.getSortOrder()).isEqualTo(8);
        assertThat(system.getStatus()).isEqualTo(SystemStatus.NORMAL);
        assertThat(system.getId()).isEqualTo(id);
        assertThat(system.getCode()).isEqualTo("arche");
        assertThat(system.getCreatedAt()).isEqualTo(createdAt);
        assertThat(system.getUpdatedAt()).isBetween(before, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        system.update("新系统", " ", " ", SystemStatus.RETIRED, 0);
        assertThat(system.getEntryUrl()).isNull();
        assertThat(system.getDescription()).isNull();
    }

    @Test
    @DisplayName("非法状态和排序失败时保留所有原有资料及更新时间")
    void invalidUpdateIsAtomic() {
        var system = newSystem();
        var updatedAt = system.getUpdatedAt();
        assertError(() -> system.update("被拒绝的新名称", "新说明", "https://example.com", null, 0),
                "INVALID_SYSTEM_STATUS");
        assertError(() -> system.update("被拒绝的新名称", "新说明", "https://example.com",
                SystemStatus.NORMAL, -1), "INVALID_SYSTEM_SORT");
        assertError(() -> system.update("被拒绝的新名称", "新说明", null, SystemStatus.NORMAL, 0),
                "SYSTEM_ENTRY_REQUIRED");
        assertThat(system.getName()).isEqualTo("系统");
        assertThat(system.getDescription()).isEqualTo("原说明");
        assertThat(system.getEntryUrl()).isNull();
        assertThat(system.getStatus()).isEqualTo(SystemStatus.NOT_LAUNCHED);
        assertThat(system.getSortOrder()).isZero();
        assertThat(system.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("系统仅在规范化名称实际改变时返回改名领域事件")
    void renameEventContainsStableSystemCodeAndNames() {
        var system = newSystem();
        assertThat(system.update(" 系统 ", "其他说明", null, SystemStatus.NOT_LAUNCHED, 1)).isEmpty();
        var event = system.update(" 新系统 ", null, null, SystemStatus.NOT_LAUNCHED, 0).orElseThrow();
        assertThat(event.systemCode()).isEqualTo("arche");
        assertThat(event.previousName()).isEqualTo("系统");
        assertThat(event.name()).isEqualTo("新系统");
        assertThat(system.update("新系统", null, null, SystemStatus.NOT_LAUNCHED, 0)).isEmpty();
        assertError(() -> system.update("失败名称", null, null, SystemStatus.NORMAL, 0), "SYSTEM_ENTRY_REQUIRED");
        assertThat(system.getName()).isEqualTo("新系统");
        assertThat(system.update("再次更名", null, null, SystemStatus.NOT_LAUNCHED, 0)
                .orElseThrow().previousName()).isEqualTo("新系统");
    }

    /** 创建内存中的聚合，不创建真实业务数据。 */
    private ManagedSystem newSystem() {
        return ManagedSystem.create("arche", "系统", "原说明", null, SystemStatus.NOT_LAUNCHED, 0);
    }

    /** 断言稳定业务错误码，不绑定完整提示文案。 */
    private void assertError(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(
                BusinessException.class, error -> assertThat(error.getCode()).isEqualTo(code)
        );
    }
}
