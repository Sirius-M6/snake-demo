package util;

import java.util.Optional;

/**
 * 存储抽象(接口):文件/内存两个实现;
 * 判定/组装配逻辑在 SaveController(规则层),本接口只做原始存取;
 * 生产注入 LocalStore,测试注入 InMemoryStore
 */
public interface Store {

    /** 写入(目录/文件名由调用方给定) */
    void saveText(String file, String content);

    /** 读取(文件缺失返回 empty) */
    Optional<String> loadText(String file);

    /** 删除 */
    void delete(String file);

    /** 是否存在 */
    boolean exists(String file);
}
