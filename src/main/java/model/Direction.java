package model;

/**
 * 方向枚举(UP/DOWN/LEFT/RIGHT);向量/禁掉头/颠倒映射
 */
public enum Direction {

    /** 上 */
    UP,
    /** 下 */
    DOWN,
    /** 左 */
    LEFT,
    /** 右 */
    RIGHT;

    /** 移动向量分量 dx(与行/列坐标系一致,移动/转向用) */
    public int dx() {
        return 0;
    }

    /** 移动向量分量 dy(与行/列坐标系一致,移动/转向用) */
    public int dy() {
        return 0;
    }

    /** 是否反向 → 禁止掉头判定(例如右行时 LEFT 为反向) */
    public boolean isOpposite(Direction other) {
        return false;
    }

    /** debuff 颠倒映射(UP↔DOWN、LEFT↔RIGHT) */
    public Direction inverted() {
        return null;
    }
}
