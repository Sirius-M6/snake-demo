package config;

import model.Difficulty;

/**
 * 难度初始速度与随分阶梯提速(数值待 QA-17)
 */
public final class SpeedConfig {

    /** 速度上限(最快档位毫秒下限)[待确认:QA-17] */
    public static final long MIN_INTERVAL_MS = 0L;

    /** 难度 → 每格毫秒(低慢/中中/高快)[待确认:QA-17,占位] */
    public static long initIntervalMs(Difficulty difficulty) {
        return 0L;
    }

    /** 阶梯提速档位表:分数阈值 → 每格毫秒,只升不降 [待确认:QA-17] */
    public static long tierFor(int score) {
        return 0L;
    }

    /** 私有构造:常量类不可实例化 */
    private SpeedConfig() {
    }
}
