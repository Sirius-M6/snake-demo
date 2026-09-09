package model;

/**
 * 豆类别枚举(小/大/金/毒/大毒);分值在 config,此处只判类别
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

    /** 是否小豆子 */
    public boolean isSmall() {
        return false;
    }

    /** 是否触发方向颠倒(仅 BIG_POISON) */
    public boolean isDebuff() {
        return false;
    }

    /** 是否正面豆(得分/救场) */
    public boolean isPositive() {
        return false;
    }

    /** 互斥组 id:大/金同组非 0(其余 0),用于大/金互斥判定(大金不同时在场) */
    public int mutualGroup() {
        return 0;
    }
}
