package com.dedsi.arche.system.infrastructure.systems.events;

import com.dedsi.arche.system.domain.permissions.IPermissionRepository;
import com.dedsi.arche.system.domain.positions.IPositionRepository;
import com.dedsi.arche.system.domain.systems.events.SystemRenamedEvent;
import com.dedsi.arche.system.domain.systems.events.SystemRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 单一同步协调器，系统名称及所有引用在同一事务中共同成功或回滚。 */
@Component
public class SystemRenamedListener {
    /* 先锁权限聚合，避免岗位配置复制旧名称。 */
    private final IPermissionRepository permissions;
    /* 后处理完整岗位聚合，保证根和全部子项一起维护版本。 */
    private final IPositionRepository positions;

    /** 注入两个领域的仓储；不直接修改子表或执行批量 JPQL 更新。 */
    public SystemRenamedListener(IPermissionRepository permissions, IPositionRepository positions) {
        this.permissions = permissions;
        this.positions = positions;
    }

    /** 发布者已持有系统行锁，监听器严格加入其事务。 */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onSystemRenamed(SystemRenamedEvent event) {
        synchronize(event.systemCode(), event.name());
    }

    /** 新登记系统继续修正历史同编码权限和岗位子项的名称，不猜测历史岗位根归属。 */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onSystemRegistered(SystemRegisteredEvent event) {
        synchronize(event.systemCode(), event.name());
    }

    /** 系统锁之后，权限按 ID 加写锁，再按岗位 ID 写入；岗位同时匹配根和子项时只更新一次。 */
    private void synchronize(String code, String name) {
        for (var permission : permissions.lockAllBySystemCode(code)) {
            permission.synchronizeSystemName(name);
        }
        permissions.flush();
        for (var position : positions.findAllReferencingSystem(code)) {
            position.synchronizeSystemName(code, name);
            positions.flush();
        }
    }
}
