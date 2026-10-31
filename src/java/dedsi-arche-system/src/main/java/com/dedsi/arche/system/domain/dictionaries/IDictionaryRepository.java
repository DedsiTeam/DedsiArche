package com.dedsi.arche.system.domain.dictionaries;

import java.util.List;
import java.util.Optional;

/** 字典聚合持久化契约，所有读取均包含完整子项，分页投影另行查询。 */
public interface IDictionaryRepository {
    /** 单条查询加载完整聚合，不存在时返回空。 */
    Optional<Dictionary> findById(String id);

    /** 按所属系统和字典编码读取完整聚合。 */
    Optional<Dictionary> findBySystemCodeAndCode(String systemCode, String code);

    /** 按 ID 排序完整加载系统下字典，供同事务系统改名使用。 */
    List<Dictionary> findAllBySystemCode(String systemCode);

    /** 检查同一系统下编码是否已存在，数据库唯一约束兜底。 */
    boolean existsBySystemCodeAndCode(String systemCode, String code);

    /** 系统删除前检查字典引用，调用方已持有系统锁。 */
    boolean existsBySystemCode(String systemCode);

    /** 保存根实体及全部子项。 */
    Dictionary save(Dictionary dictionary);

    /** 删除字典及子项，不删除所属系统。 */
    void delete(Dictionary dictionary);

    /** 同步约束与版本检查，让响应包含持久化后的根版本。 */
    void flush();
}
