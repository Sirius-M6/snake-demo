package config;

import model.BeanType;

/**
 * 豆子:分值/蛇身效果/在场时限/生成概率(核心配置,BR-20~BR-29;PRD v1.7,2026-09-10)
 * v1.7 调整:生成概率 金:大=1:1、毒:大毒=1:3、正:负=3:1;废止大/金互斥与旧权重(小:金:大=3:1:1、毒:大毒=3:1)
 */
public final class BeanConfig {

    /** 场上豆子总数上限(含小豆子;小豆 1 颗在场时,非小豆同时最多 2 颗)(BR-28) */
    public static final int MAX_ON_BOARD = 3;

    /** 负面豆(毒豆子/大毒豆子)在场 3 秒未吃自动消失 */
    public static final long NEGATIVE_LIFETIME_MS = 3_000;

    /** 正面豆(减负金豆/大豆子)在场 5 秒未吃自动消失(BR-28;v1.7 ) */
    public static final long POSITIVE_LIFETIME_MS = 5_000;

    /** 非小豆子被吃/超时消失后延迟补刷时长(BR-25②) */
    public static final long REFILL_DELAY_MS = 3_000;

    /** 大毒豆方向颠倒持续 2 秒(再吃重置) */
    public static final long DEBUFF_MS = 2_000;

    /** 空格 < 10 时负面豆(毒豆子、大毒豆子,即 debuff 类)不参与抽取,仅抽正面豆(BR-25③) */
    public static final int DEBUFF_SPAWN_FREE_CELL_LIMIT = 10;

    /** 正面池权重:[减负金豆, 大豆子] = 1:1(BR-29①;v1.7 修订) */
    public static final int[] POSITIVE_WEIGHTS = {1, 1};

    /** 负面池权重:[毒豆子, 大毒豆子] = 1:3(BR-29②;v1.7 修订) */
    public static final int[] NEGATIVE_WEIGHTS = {1, 3};

    /** 正/负面池生成比例 3:1(抽取权重 {3,1})(BR-29③;QA-19 已答复 9.10) */
    public static final int POSITIVE_TO_NEGATIVE = 3;

    /** 分值表:小 +1、大 +6、金 +6、毒 -6、大毒 -10 */
    public static int scoreOf(BeanType type) {
        switch (type) {
            case SMALL:
                return 1;
            case BIG:
            case GOLD:
                return 6;
            case POISON:
                return -6;
            case BIG_POISON:
                return -10;
            default:
                return 0; // 不可达:枚举全覆盖
        }
    }

    /** 蛇身效果表:尾部节数变化(正=增长/负=缩短);金豆固定 -2(消除 2 节,2026-09-10 已确定);毒/大毒 0=不变 */
    public static int tailEffectOf(BeanType type) {
        switch (type) {
            case SMALL:
            case BIG:
                return 1;
            case GOLD:
                return -2;
            case POISON:
            case BIG_POISON:
            default:
                return 0;
        }
    }

    /**
     * 在场时限表:正面豆(金/大)5s、负面豆(毒/大毒)3s;小豆不限时,返回 Long.MAX_VALUE 哨兵(BR-28);
     */
    public static long duration(BeanType type) {
        if (type == BeanType.SMALL) {
            return Long.MAX_VALUE;
        }
        return type.isPositive() ? POSITIVE_LIFETIME_MS : NEGATIVE_LIFETIME_MS;
    }

    /** 私有构造:常量类不可实例化 */
    private BeanConfig() {
    }
}
