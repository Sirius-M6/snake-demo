package util;

import java.util.Optional;

/**
 * Store 的文件系统实现(生产用;B 模块维护);
 * 目录与文件名来自 SaveConfig(默认用户目录隐藏子目录)
 */
public class LocalStore implements Store {

    /** 写入(目录/文件名由调用方给定) */
    @Override
    public void saveText(String file, String content) {
    }

    /** 读取(文件缺失返回 empty) */
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
