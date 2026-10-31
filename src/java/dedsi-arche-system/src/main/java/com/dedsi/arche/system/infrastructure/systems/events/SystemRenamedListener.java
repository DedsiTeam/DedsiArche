package com.dedsi.arche.system.infrastructure.systems.events;

import com.dedsi.arche.system.domain.permissions.IPermissionRepository;
import com.dedsi.arche.system.domain.dictionaries.IDictionaryRepository;
import com.dedsi.arche.system.domain.organizations.IOrganizationRepository;
import com.dedsi.arche.system.domain.menus.IMenuRepository;
import com.dedsi.arche.system.domain.positions.IPositionRepository;
import com.dedsi.arche.system.domain.systems.events.SystemRenamedEvent;
import com.dedsi.arche.system.domain.systems.events.SystemRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 单一同步协调器，系统名称及实时引用在同一事务中共同成功或回滚；UserPosition 保留配置时历史快照。 */
@Component
public class SystemRenamedListener {
    /* 先锁权限聚合，避免岗位配置复制旧名称。 */
    private final IPermissionRepository permissions;
    /* 后处理完整岗位聚合，保证根和全部子项一起维护版本。 */
    private final IPositionRepository positions;
    /* 字典按 ID 同步，之后处理组织，保持固定写入顺序。 */
    private final IDictionaryRepository dictionaries;
    /* 组织节点按 ID 同步，不重建或级联修改树。 */
    private final IOrganizationRepository organizations;
    /* 菜单最后按 ID 同步系统名称。 */
    private final IMenuRepository menus;

    /** 注入引用系统名称的领域仓储；不直接修改子表或执行批量 JPQL 更新。 */
    public SystemRenamedListener(
            IPermissionRepository permissions, IPositionRepository positions, IDictionaryRepository dictionaries,
            IOrganizationRepository organizations, IMenuRepository menus
    ) {
        this.permissions = permissions;
        this.positions = positions;
        this.dictionaries = dictionaries;
        this.organizations = organizations;
        this.menus = menus;
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

    /** 系统锁之后依次处理权限、岗位、字典、组织、菜单，各领域按 ID 顺序写入；岗位根和子项同时匹配时只更新一次。 */
    private void synchronize(String code, String name) {
        for (var permission : permissions.lockAllBySystemCode(code)) {
            permission.synchronizeSystemName(name);
        }
        permissions.flush();
        for (var position : positions.findAllReferencingSystem(code)) {
            position.synchronizeSystemName(code, name);
            positions.flush();
        }
        for (var dictionary : dictionaries.findAllBySystemCode(code)) {
            dictionary.synchronizeSystemName(code, name);
            dictionaries.flush();
        }
        for (var organization : organizations.findAllBySystemCode(code)) {
            organization.synchronizeSystemName(code, name);
            organizations.flush();
        }
        for (var menu : menus.findAllBySystemCode(code)) {
            menu.synchronizeSystemName(code, name);
            menus.flush();
        }
    }
}
