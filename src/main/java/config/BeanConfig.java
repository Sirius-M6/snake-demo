package config;

import model.BeanType;

/**
 * 豆子:分值/蛇身效果/时限/比例/互斥(核心配置,BR-20~BR-29)
 */
public final class BeanConfig {

    /** 场上同时最多 3 颗豆(BR-28) */
    public static final int MAX_ON_BOARD = 3;

    /** 毒豆子/大毒豆子在场 3 秒未吃消失(BR-28) */
    public static final long TOXIC_LIFETIME_MS = 3_000;

    /** 大豆子/减负金豆在场 5 秒未吃消失(BR-28) */
    public static final long BIG_GOLD_LIFETIME_MS = 5_000;

    /** 非小豆子空位延迟补刷时长(BR-25) */
    public static final long REFILL_DELAY_MS = 3_000;

    /** 大毒豆方向颠倒持续 2 秒(再吃重置) */
    public static final long DEBUFF_MS = 2_000;

    /** 空格 < 10 时不补刷 debuff 类豆(BR-25) */
    public static final int DEBUFF_SPAWN_FREE_CELL_LIMIT = 10;

    /** 正面池权重:小豆子:减负金豆:大豆子 = 3:1:1(BR-29) */
    public static final int[] POSITIVE_WEIGHTS = {3, 1, 1};

    /** 负面池权重:毒豆子:大毒豆子 = 3:1(BR-29) */
    public static final int[] NEGATIVE_WEIGHTS = {3, 1};

    /** 正/负面池全局比例 2:1 [待确认:QA-19,开发期占位] */
    public static final int POSITIVE_TO_NEGATIVE = 2;

    /** 分值表:小 +1、大 +6、金 +6、毒 -6、大毒 -10 */
    public static int scoreOf(BeanType type) {
        return 0;
    }

    /** 蛇身效果表:+1 节 / 消除 2~3 节(金,随机量由 BeanController 掷)/ 不变 */
    public static int tailEffectOf(BeanType type) {
        return 0;
    }

    /** 小豆子不限时,无在场时限常量(BR-28) */
    public static long duration() {
        return 0L;
    }

    /** 私有构造:常量类不可实例化 */
    private BeanConfig() {
    }
}
