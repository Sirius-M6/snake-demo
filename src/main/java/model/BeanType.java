package model;

/**
 * 豆类别枚举(小/大/金/毒/大毒);分值在 config,此处只判类别
 * v1.7:小豆子独立生成;非小豆子按 BR-29 概率抽取(正面池/负面池)
 */
public enum BeanType {

    /** 小豆子 */
    SMALL,
    /** 大豆子 */
    BIG,
    /** 减负金豆 */
    GOLD,
    /** 毒豆子 */
    POISON,
    /** 大毒豆子 */
    BIG_POISON;

    /** 是否小豆子(独立生成轨道,场上恒为 1 颗) */
    public boolean isSmall() {
        return this == SMALL;
    }

    /** 是否触发方向颠倒(仅 BIG_POISON) */
    public boolean isDebuff() {
        return this == BIG_POISON;
    }

    /** 是否正面豆(减负金豆/大豆子;在场 5 秒) */
    public boolean isPositive() {
        return this == GOLD || this == BIG;
    }

    /** 是否负面豆(毒豆子/大毒豆子;在场 3 秒;空格 < 10 时排除) */
    public boolean isNegative() {
        return this == POISON || this == BIG_POISON;
    }
}
