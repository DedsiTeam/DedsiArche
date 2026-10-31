package com.dedsi.arche.system.infrastructure.loginaudits.security;

import com.dedsi.arche.system.domain.loginaudits.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 独立 Spring 事务代理：成功加入会话事务，失败另开事务。 */
@Component
public class LoginAuditWriter {
    /* 仅新增事实的仓储。 */
    private final ILoginAuditRepository audits;

    public LoginAuditWriter(ILoginAuditRepository audits) {
        this.audits = audits;
    }

    /** 写入失败向外传播，刷新会话和审计必须共同提交才允许返回令牌。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void success(LoginAuditContext context) {
        audits.append(context.fact(LoginAuditReason.LOGIN_SUCCEEDED));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                context.committed();
            }
        });
    }

    /** 失败事实独立提交，提交失败由调用方在事务代理之外捕获。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(LoginAuditContext context, LoginAuditReason reason) {
        audits.append(context.fact(reason));
    }
}
