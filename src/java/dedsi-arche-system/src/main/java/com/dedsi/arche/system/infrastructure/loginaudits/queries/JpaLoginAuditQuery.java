package com.dedsi.arche.system.infrastructure.loginaudits.queries;

import com.dedsi.arche.system.domain.loginaudits.*;
import com.dedsi.arche.system.domain.loginaudits.query.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** 参数绑定的审计只读查询，不联表恢复当前用户资料，保留历史事实。 */
@Repository
@Transactional(readOnly = true)
public class JpaLoginAuditQuery implements ILoginAuditQuery {
    private final EntityManager em;

    public JpaLoginAuditQuery(EntityManager em) {
        this.em = em;
    }

    @Override
    public LoginAuditPage search(
            String keyword, String loginAccount, String displayName, String clientIp,
            LoginAuditResult result, LoginAuditReason reasonCode, LocalDateTime from, LocalDateTime to,
            int page, int size
    ) {
        var where = new StringBuilder(" from LoginAudit a where 1 = 1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (!keyword.isBlank()) {
            where.append(" and (locate(:keyword, lower(coalesce(a.loginAccount, ''))) > 0"
                    + " or locate(:keyword, lower(coalesce(a.displayName, ''))) > 0)");
            parameters.put("keyword", keyword.toLowerCase(Locale.ROOT));
        }
        if (!loginAccount.isBlank()) {
            where.append(" and locate(:loginAccount, lower(coalesce(a.loginAccount, ''))) > 0");
            parameters.put("loginAccount", loginAccount.toLowerCase(Locale.ROOT));
        }
        if (!displayName.isBlank()) {
            where.append(" and locate(:displayName, lower(coalesce(a.displayName, ''))) > 0");
            parameters.put("displayName", displayName.toLowerCase(Locale.ROOT));
        }
        if (!clientIp.isBlank()) {
            where.append(" and a.clientIp = :clientIp");
            parameters.put("clientIp", clientIp);
        }
        if (result != null) {
            where.append(" and a.result = :result");
            parameters.put("result", result);
        }
        if (reasonCode != null) {
            where.append(" and a.reasonCode = :reasonCode");
            parameters.put("reasonCode", reasonCode);
        }
        if (from != null) {
            where.append(" and a.loginTime >= :fromTime");
            parameters.put("fromTime", from);
        }
        if (to != null) {
            where.append(" and a.loginTime < :toTime");
            parameters.put("toTime", to);
        }
        var rows = em.createQuery("select a" + where + " order by a.loginTime desc, a.id desc", LoginAudit.class);
        var count = em.createQuery("select count(a)" + where, Long.class);
        parameters.forEach((key, value) -> {
            rows.setParameter(key, value);
            count.setParameter(key, value);
        });
        return new LoginAuditPage(rows.setFirstResult((page - 1) * size).setMaxResults(size)
                .getResultList().stream().map(LoginAuditDto::from).toList(), count.getSingleResult(), page, size);
    }

    @Override
    public Optional<LoginAuditDto> findById(String id) {
        return Optional.ofNullable(em.find(LoginAudit.class, id)).map(LoginAuditDto::from);
    }
}
