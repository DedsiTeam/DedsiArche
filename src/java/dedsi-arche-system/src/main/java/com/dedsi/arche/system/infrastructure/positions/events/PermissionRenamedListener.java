package com.dedsi.arche.system.infrastructure.positions.events;

import com.dedsi.arche.system.domain.permissions.events.PermissionRenamedEvent;
import com.dedsi.arche.system.domain.positions.IPositionRepository;
import com.dedsi.arche.system.domain.menus.IMenuRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 原生 Spring 同步事件监听器；权限更名和岗位引用更新必须共同提交或回滚。 */
@Component
public class PermissionRenamedListener {
    /* 完整岗位聚合仓储，子实体不独立写入。 */
    private final IPositionRepository positions;
    /* 岗位之后更新菜单，保持全局写入顺序。 */
    private final IMenuRepository menus;

    /** 注入聚合仓储。 */
    public PermissionRenamedListener(IPositionRepository positions, IMenuRepository menus) {
        this.positions = positions;
        this.menus = menus;
    }

    /**
     * 必须加入发布者事务，不使用异步或提交后监听。
     * 按岗位 ID 顺序刷新，约束/版本失败向上传播，使权限改名也回滚。
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onPermissionRenamed(PermissionRenamedEvent event) {
        for (var position : positions.findAllByPermissionId(event.permissionId())) {
            position.synchronizePermissionName(event.permissionId(), event.name());
            positions.flush();
        }
        for (var menu : menus.findAllByPermissionId(event.permissionId())) {
            menu.synchronizePermissionName(event.permissionId(), event.name());
            menus.flush();
        }
    }
}
