package com.dedsi.arche.system.domain.users;

import com.dedsi.arche.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

/** 直接验证用户聚合的业务行为，不加载 Spring 或持久化设施。 */
class UserUnitTests {

    @Test
    @DisplayName("注册规范化账号及姓名，生成独立标识并默认启用")
    void registrationInitializesUser() {
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        User user = User.register("  ADMIN_I  ", "  张三  ", "test-hash");

        assertThat(user.getUsername()).isEqualTo("admin_i");
        assertThat(user.getDisplayName()).isEqualTo("张三");
        assertThat(user.getId()).isNotNull();
        assertThat(user.getId()).isNotEqualTo(newUser().getId());
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getCredentialVersion()).isZero();
        assertThat(user.getPasswordHash()).isEqualTo("test-hash");
        assertThat(user.getCreatedAt()).isBetween(before, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        assertThatCode(user::ensureEnabled).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("修改姓名去除空白，接受 100 字符且不改变身份信息")
    void renamePreservesIdentity() {
        User user = newUser();
        var id = user.getId();
        var createdAt = user.getCreatedAt();

        user.rename("  " + "名".repeat(100) + "  ");

        assertThat(user.getDisplayName()).isEqualTo("名".repeat(100));
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    @DisplayName("注册和修改都拒绝空姓名，失败修改保留原姓名")
    void blankNameIsRejected(String name) {
        User user = newUser();

        assertBusinessError(() -> user.rename(name), "INVALID_DISPLAY_NAME");
        assertBusinessError(() -> User.register("alice", name, "test-hash"), "INVALID_DISPLAY_NAME");
        assertThat(user.getDisplayName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("姓名超过 100 字符被拒绝")
    void oversizedNameIsRejected() {
        assertBusinessError(() -> newUser().rename("名".repeat(101)), "INVALID_DISPLAY_NAME");
    }

    @Test
    @DisplayName("联系方式去除空白并接受手机号长度边界")
    void contactsAreNormalized() {
        User user = newUser();
        user.updateContact(" 1234567 ", " alice@example.com ");
        assertThat(user.getPhone()).isEqualTo("1234567");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");

        user.updateContact("+123456789012345", "a".repeat(242) + "@example.com");
        assertThat(user.getPhone()).isEqualTo("+123456789012345");
        assertThat(user.getEmail()).hasSize(254);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("领域层 null 或空白联系方式表示清除")
    void emptyContactsClearExistingValues(String empty) {
        User user = newUser();
        user.updateContact("1234567", "alice@example.com");

        user.updateContact(empty, empty);

        assertThat(user.getPhone()).isNull();
        assertThat(user.getEmail()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "1234567890123456", "+123456", "12-34567", "abcdefg"})
    @DisplayName("非法手机号被拒绝且不覆盖原有联系方式")
    void invalidPhonePreservesContacts(String phone) {
        User user = newUser();
        user.updateContact("1234567", "alice@example.com");

        assertBusinessError(() -> user.updateContact(phone, "new@example.com"), "INVALID_PHONE");

        assertThat(user.getPhone()).isEqualTo("1234567");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice", "alice@example", "@example.com", "a b@example.com", "a@@example.com"})
    @DisplayName("非法邮箱被拒绝，手机号也不能被部分更新")
    void invalidEmailPreservesContacts(String email) {
        User user = newUser();
        user.updateContact("1234567", "alice@example.com");

        assertBusinessError(() -> user.updateContact("7654321", email), "INVALID_EMAIL");

        assertThat(user.getPhone()).isEqualTo("1234567");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("邮箱超过 254 字符被拒绝")
    void oversizedEmailIsRejected() {
        assertBusinessError(
                () -> newUser().updateContact(null, "a".repeat(243) + "@example.com"),
                "INVALID_EMAIL"
        );
    }

    @Test
    @DisplayName("停用阻止领域操作，重新启用后恢复")
    void disabledUserCannotPassGuard() {
        User user = newUser();
        user.setEnabled(false);
        assertBusinessError(user::ensureEnabled, "USER_DISABLED");

        user.setEnabled(true);
        assertThatCode(user::ensureEnabled).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("每次重置密码都递增凭据版本，其他资料不变")
    void resetPasswordInvalidatesPreviousCredentialVersion() {
        User user = newUser();
        var id = user.getId();
        user.resetPassword("replacement-test-hash");
        assertThat(user.getPasswordHash()).isEqualTo("replacement-test-hash");
        assertThat(user.getCredentialVersion()).isEqualTo(1);

        user.resetPassword("replacement-test-hash");
        assertThat(user.getCredentialVersion()).isEqualTo(2);
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.isEnabled()).isTrue();
    }

    /** 创建内存中的测试聚合，哈希仅为占位值，不涉及真实账户。 */
    private User newUser() {
        return User.register("alice", "Alice", "test-hash");
    }

    /** 核对公开业务错误码，避免将实现细节或完整提示文案作为契约。 */
    private void assertBusinessError(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
