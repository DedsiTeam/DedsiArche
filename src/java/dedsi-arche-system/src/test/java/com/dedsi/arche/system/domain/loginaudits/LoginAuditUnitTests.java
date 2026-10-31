package com.dedsi.arche.system.domain.loginaudits;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** 纯领域测试，不启动 Spring、数据库、网络或容器。 */
class LoginAuditUnitTests {
    private final LocalDateTime time = LocalDateTime.of(2026, 10, 31, 12, 0);

    @Test
    void successDerivesResultAndPreservesIdentitySnapshot() {
        UUID user = UUID.randomUUID();
        var audit = LoginAudit.record(time, LoginAuditReason.LOGIN_SUCCEEDED, " alice ", user,
                "历史姓名", "127.0.0.1", "Browser", "trace");
        assertThat(audit.getId()).matches("[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(audit.getResult()).isEqualTo(LoginAuditResult.SUCCESS);
        assertThat(audit.getFailureMessage()).isNull();
        assertThat(audit.getLoginAccount()).isEqualTo("alice");
        assertThat(audit.getUserId()).isEqualTo(user);
        assertThat(audit.getDisplayName()).isEqualTo("历史姓名");
        assertThat(audit.getLoginTime()).isEqualTo(time);
    }

    @Test
    void failuresUseOnlyControlledMessagesAndMayHaveNoUser() {
        for (var reason : LoginAuditReason.values()) {
            if (reason == LoginAuditReason.LOGIN_SUCCEEDED) {
                continue;
            }
            var audit = LoginAudit.record(time, reason, null, null, null, null, null, null);
            assertThat(audit.getResult()).isEqualTo(LoginAuditResult.FAILURE);
            assertThat(audit.getFailureMessage()).isIn("账号或密码错误", "用户已停用", "登录请求格式或参数无效", "登录服务暂时不可用");
            assertThat(audit.getReasonCode()).isEqualTo(reason);
            assertThat(audit.getUserId()).isNull();
            assertThat(audit.getLoginAccount()).isNull();
        }
    }

    @Test
    void untrustedMetadataIsBoundedAndControlCharactersRemoved() {
        var audit = LoginAudit.record(time, LoginAuditReason.BAD_CREDENTIALS, "a".repeat(210), null,
                "n".repeat(110), "i".repeat(80), "u".repeat(600), "t".repeat(80));
        assertThat(audit.getLoginAccount()).hasSize(200);
        assertThat(audit.getDisplayName()).hasSize(100);
        assertThat(audit.getClientIp()).hasSize(64);
        assertThat(audit.getUserAgent()).hasSize(512);
        assertThat(audit.getTraceId()).hasSize(64);
        assertThat(LoginAudit.sanitize("  a\n\r\t\u0000\u202Eb  ", 100)).isEqualTo("ab");
        assertThat(LoginAudit.sanitize(" \n ", 100)).isNull();
        assertThat(LoginAudit.sanitize("😀".repeat(4), 3)).isEqualTo("😀😀😀");
    }

    @Test
    void oneAttemptMayReuseItsIdButNeverInferSuccessWithoutUser() {
        var failed = LoginAudit.record(time, LoginAuditReason.SERVICE_ERROR, null, null, null, null, null, null);
        var retry = LoginAudit.recordWithId(failed.getId(), time, LoginAuditReason.SERVICE_ERROR,
                null, null, null, null, null, null);
        assertThat(retry.getId()).isEqualTo(failed.getId());
        assertThatThrownBy(() -> LoginAudit.record(time, LoginAuditReason.LOGIN_SUCCEEDED,
                null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LoginAudit.recordWithId("bad", time, LoginAuditReason.SERVICE_ERROR,
                null, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiredFactFieldsCannotBeMissing() {
        assertThatThrownBy(() -> LoginAudit.record(null, LoginAuditReason.SERVICE_ERROR,
                null, null, null, null, null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> LoginAudit.record(time, null,
                null, null, null, null, null, null)).isInstanceOf(NullPointerException.class);
    }
}
