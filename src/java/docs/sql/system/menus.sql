-- 截至Flyway V15菜单结构快照，仅供阅读；执行入口为增量迁移。
-- 菜单独立节点聚合；类型、结构及关联配置不直接构成运行时鉴权。
CREATE TABLE sys_menus (
    id varchar(26) PRIMARY KEY,
    system_code varchar(100) NOT NULL,
    system_name varchar(100) NOT NULL,
    parent_id varchar(26),
    code varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    menu_type varchar(20) NOT NULL,
    permission_id varchar(26),
    permission_code varchar(100),
    permission_name varchar(100),
    route_path varchar(2048),
    component_path varchar(255),
    sort_order integer NOT NULL DEFAULT 0,
    description varchar(500),
    visible boolean NOT NULL DEFAULT true,
    enabled boolean NOT NULL DEFAULT true,
    cache boolean NOT NULL DEFAULT false,
    affix boolean NOT NULL DEFAULT false,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_menus_system_code UNIQUE (system_code, code),
    CONSTRAINT uk_menus_system_id UNIQUE (system_code, id),
    CONSTRAINT fk_menus_system FOREIGN KEY (system_code) REFERENCES sys_systems(code) ON DELETE RESTRICT,
    CONSTRAINT fk_menus_parent FOREIGN KEY (system_code, parent_id)
        REFERENCES sys_menus(system_code, id) ON DELETE RESTRICT,
    CONSTRAINT fk_menus_permission FOREIGN KEY (permission_id) REFERENCES sys_permissions(id) ON DELETE RESTRICT,
    CONSTRAINT ck_menus_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_menus_not_self CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT ck_menus_permission CHECK (
        (permission_id IS NULL AND permission_code IS NULL AND permission_name IS NULL)
        OR (permission_id IS NOT NULL AND permission_code IS NOT NULL AND permission_name IS NOT NULL)
    ),
    CONSTRAINT ck_menus_type CHECK (
        (menu_type = 'DIRECTORY' AND route_path IS NULL AND component_path IS NULL AND NOT cache AND NOT affix)
        OR (menu_type = 'PAGE' AND route_path IS NOT NULL AND char_length(route_path) <= 500 AND component_path IS NOT NULL)
        OR (menu_type = 'EXTERNAL' AND route_path IS NOT NULL AND component_path IS NULL AND NOT cache AND NOT affix)
    )
);

CREATE UNIQUE INDEX uk_menus_page_route ON sys_menus(system_code, route_path) WHERE menu_type = 'PAGE';
CREATE INDEX idx_menus_parent ON sys_menus(parent_id);
CREATE INDEX idx_menus_tree ON sys_menus(system_code, parent_id, sort_order, code, id);
CREATE INDEX idx_menus_permission ON sys_menus(permission_id);

COMMENT ON TABLE sys_menus IS '菜单独立节点聚合，只有目录可承载下级；配置管理不直接授予接口权限';
COMMENT ON COLUMN sys_menus.id IS '菜单 ULID 主键';
COMMENT ON COLUMN sys_menus.system_code IS '所属系统编码，创建后不可修改';
COMMENT ON COLUMN sys_menus.system_name IS '系统名称，系统改名同事务同步';
COMMENT ON COLUMN sys_menus.parent_id IS '同系统上级目录标识，空值为顶级';
COMMENT ON COLUMN sys_menus.code IS '系统内唯一菜单编码，创建后不可修改';
COMMENT ON COLUMN sys_menus.name IS '菜单显示名称';
COMMENT ON COLUMN sys_menus.menu_type IS '菜单类型 DIRECTORY目录 PAGE站内页面 EXTERNAL外链';
COMMENT ON COLUMN sys_menus.permission_id IS '可空权限引用，权限快照三字段同时空或有值';
COMMENT ON COLUMN sys_menus.permission_code IS '服务端读取的关联权限编码';
COMMENT ON COLUMN sys_menus.permission_name IS '关联权限名称，权限改名同事务同步';
COMMENT ON COLUMN sys_menus.route_path IS 'PAGE站内路径最多500字符或EXTERNAL完整HTTP(S)链接最多2048字符，目录为空';
COMMENT ON COLUMN sys_menus.component_path IS 'PAGE逻辑组件路径，不执行任意模块加载，其他类型为空';
COMMENT ON COLUMN sys_menus.sort_order IS '同层非负排序，默认0';
COMMENT ON COLUMN sys_menus.description IS '可空菜单说明';
COMMENT ON COLUMN sys_menus.visible IS '是否显示，默认true，隐藏不代表禁止访问';
COMMENT ON COLUMN sys_menus.enabled IS '是否启用，默认true';
COMMENT ON COLUMN sys_menus.cache IS '是否缓存页面，API创建PAGE默认true，非PAGE必须false';
COMMENT ON COLUMN sys_menus.affix IS '页面固定标签标记，默认false，非PAGE必须false';
COMMENT ON COLUMN sys_menus.created_at IS '创建时间，北京时间Asia/Shanghai';
COMMENT ON COLUMN sys_menus.updated_at IS '更新时间，北京时间Asia/Shanghai';
COMMENT ON COLUMN sys_menus.version IS '乐观并发版本，实际变更递增';
