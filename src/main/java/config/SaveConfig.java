package config;

import java.io.File;

/**
 * 存档与最高分文件路径(B 模块维护)
 * 默认用户目录隐藏子目录,避免打包后不可写;单存档、最高分按地图
 */
public final class SaveConfig {

    /** 存档目录(默认用户目录下的隐藏子目录;由 LocalStore 首次写入时按需自动创建) */
    public static final String SAVE_DIR = System.getProperty("user.home") + File.separator + ".snake-demo";

    /** 单存档文件名 */
    public static final String SAVE_FILE = "save.txt";

    /** 最高分文件名 */
    public static final String HIGH_SCORE_FILE = "scores.txt";

    /** 私有构造:常量类不可实例化 */
    private SaveConfig() {
    }
}
