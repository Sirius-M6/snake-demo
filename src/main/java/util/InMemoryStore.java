package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Store 的内存实现(仅测试用,不进生产装配);
 * 内部 Map 存储;SaveController 单测不碰磁盘
 */
public class InMemoryStore implements Store {

    /** 骨架补充(原因:骨架注释声明"内部 Map 存储"但未声明字段;须在《详细设计说明书》变更记录登记) */
    private final Map<String, String> files = new HashMap<>();

    /** 写入 */
    @Override
    public void saveText(String file, String content) {
        files.put(file, content);
    }

    /** 读取(缺失返回 empty) */
    @Override
    public Optional<String> loadText(String file) {
        return Optional.ofNullable(files.get(file));
    }

    /** 删除 */
    @Override
    public void delete(String file) {
        files.remove(file);
    }

    /** 是否存在 */
    @Override
    public boolean exists(String file) {
        return files.containsKey(file);
    }
}
