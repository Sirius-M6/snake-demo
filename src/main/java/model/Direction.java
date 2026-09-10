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
        return switch (this) {
            case LEFT -> -1;
            case RIGHT -> 1;
            case UP, DOWN -> 0;
        };
    }

    /** 移动向量分量 dy(与行/列坐标系一致,移动/转向用) */
    public int dy() {
        return switch (this) {
            case UP -> -1;
            case DOWN -> 1;
            case LEFT, RIGHT -> 0;
        };
    }

    /** 是否反向 → 禁止掉头判定(例如右行时 LEFT 为反向) */
    public boolean isOpposite(Direction other) {
        return switch (this) {
            case UP -> other == DOWN;
            case DOWN -> other == UP;
            case LEFT -> other == RIGHT;
            case RIGHT -> other == LEFT;
        };
    }

    /** debuff 颠倒映射(UP↔DOWN、LEFT↔RIGHT) */
    public Direction inverted() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }
}
