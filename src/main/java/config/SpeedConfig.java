package config;

import model.Difficulty;

/**
 * 难度初始速度与随分阶梯提速(QA-17 答复前:以下为开发组默认量化值,答复后仅改本类)
 */
public final class SpeedConfig {

    /** 速度上限(最快档位毫秒下限)[QA-17 答复前默认量化] */
    public static final long MIN_INTERVAL_MS = 60L;

    /** 阶梯提速分数阈值表(升序;score 达到阈值即进入该档)[QA-17 答复前默认量化] */
    private static final int[] TIER_SCORE_THRESHOLDS = {0, 10, 20, 30, 40, 50, 60, 80, 100};

    /** 各档每格毫秒(与阈值表等长,逐档递减,末档 = MIN_INTERVAL_MS)[QA-17 答复前默认量化] */
    private static final long[] TIER_INTERVALS_MS = {200L, 180L, 165L, 150L, 135L, 120L, 105L, 90L, 60L};

    /** 难度 → 每格毫秒(低 200 慢 / 中 150 / 高 100 快)[QA-17 答复前默认量化] */
    public static long initIntervalMs(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 200L;
            case HARD:
                return 100L;
            case NORMAL:
            default:
                return 150L;
        }
    }

    /** 阶梯提速档位表:分数阈值 → 每格毫秒,只升不降 [QA-17 答复前默认量化] */
    public static long tierFor(int score) {
        long interval = TIER_INTERVALS_MS[0];
        for (int i = 0; i < TIER_SCORE_THRESHOLDS.length; i++) {
            if (score >= TIER_SCORE_THRESHOLDS[i]) {
                interval = TIER_INTERVALS_MS[i];
            }
        }
        return interval;
    }

    /** 私有构造:常量类不可实例化 */
    private SpeedConfig() {
    }
}
