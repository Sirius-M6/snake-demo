package util;

import java.util.Optional;

/**
 * Store 的内存实现(仅测试用,不进生产装配);
 * 内部 Map 存储;SaveController 单测不碰磁盘
 */
public class InMemoryStore implements Store {

    /** 写入 */
    @Override
    public void saveText(String file, String content) {
    }

    /** 读取(缺失返回 empty) */
    @Override
    public Optional<String> loadText(String file) {
        return Optional.empty();
    }

    /** 删除 */
    @Override
    public void delete(String file) {
    }

    /** 是否存在 */
    @Override
    public boolean exists(String file) {
        return false;
    }
}
