-- 字典领域完整结构快照，截至 Flyway V13；仅供阅读，不执行到现有数据库。

CREATE TABLE sys_dictionaries (
    id varchar(26) PRIMARY KEY,
    system_code varchar(100) NOT NULL,
    system_name varchar(100) NOT NULL,
    code varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(500),
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_dictionaries_system_code UNIQUE (system_code, code),
    CONSTRAINT fk_dictionaries_system FOREIGN KEY (system_code)
        REFERENCES sys_systems (code) ON DELETE RESTRICT
);

CREATE TABLE sys_dictionary_items (
    id varchar(26) PRIMARY KEY,
    dictionary_id varchar(26) NOT NULL,
    code varchar(100) NOT NULL,
    name varchar(100) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    is_default boolean NOT NULL DEFAULT false,
    enabled boolean NOT NULL DEFAULT true,
    CONSTRAINT fk_dictionary_items_dictionary FOREIGN KEY (dictionary_id)
        REFERENCES sys_dictionaries (id) ON DELETE CASCADE,
    CONSTRAINT uk_dictionary_items_code UNIQUE (dictionary_id, code),
    CONSTRAINT ck_dictionary_items_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_dictionary_items_default_enabled CHECK (NOT is_default OR enabled)
);

CREATE INDEX ix_dictionary_items_sort ON sys_dictionary_items (dictionary_id, sort_order, code, id);

-- 同一字典最多一个默认项由聚合最终状态校验、系统锁及根乐观锁共同维护。
-- 不添加立即检查的部分唯一索引，避免合法的默认项整体切换因 SQL 执行顺序误报冲突。
COMMENT ON TABLE sys_dictionaries IS '字典聚合根，所属系统内编码唯一，与全部字典项共同保存和并发控制';
COMMENT ON TABLE sys_dictionary_items IS '字典子实体，随字典整存整取；同一字典最多一个默认项且默认项必须启用';
COMMENT ON COLUMN sys_dictionaries.id IS '字典主键，后端生成的26位ULID';
COMMENT ON COLUMN sys_dictionaries.system_code IS '所属系统编码，外键关联sys_systems(code)，创建后不可修改';
COMMENT ON COLUMN sys_dictionaries.system_name IS '所属系统当前名称，由服务端读取，系统改名通过同步领域事件同事务维护';
COMMENT ON COLUMN sys_dictionaries.code IS '字典编码，同一系统内唯一且区分大小写，创建后不可修改';
COMMENT ON COLUMN sys_dictionaries.name IS '字典名称';
COMMENT ON COLUMN sys_dictionaries.description IS '字典说明，可空，最多500字符';
COMMENT ON COLUMN sys_dictionaries.enabled IS '字典整体启用状态，默认true；停用时业务读取返回不存在，不改变子项启用配置';
COMMENT ON COLUMN sys_dictionaries.created_at IS '创建时间，北京时间Asia/Shanghai，不含时区偏移';
COMMENT ON COLUMN sys_dictionaries.updated_at IS '最近修改时间，北京时间Asia/Shanghai；根、子项或系统名称实际变化时更新';
COMMENT ON COLUMN sys_dictionaries.version IS '整个字典聚合的JPA乐观锁版本，默认0，保护根和全部子项并发更新';
COMMENT ON COLUMN sys_dictionary_items.id IS '字典项主键，后端生成的26位ULID';
COMMENT ON COLUMN sys_dictionary_items.dictionary_id IS '所属字典ULID，外键关联sys_dictionaries.id，删除字典时级联删除子项';
COMMENT ON COLUMN sys_dictionary_items.code IS '项编码，同一字典内唯一且区分大小写，创建后不可修改';
COMMENT ON COLUMN sys_dictionary_items.name IS '字典项名称';
COMMENT ON COLUMN sys_dictionary_items.sort_order IS '非负展示排序，默认0；相同时按编码、ID稳定排序';
COMMENT ON COLUMN sys_dictionary_items.is_default IS '是否默认项，默认false；最多一个默认项由聚合校验，默认项必须启用';
COMMENT ON COLUMN sys_dictionary_items.enabled IS '字典项是否启用，默认true；业务读取只返回启用项';
