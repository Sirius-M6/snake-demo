package model;

/**
 * 场上豆实例:类型/位置/出生时刻
 */
public class Bean {

    /** 类别 */
    public BeanType type;

    /** 位置 */
    public Point pos;

    /** 出生时刻(游戏时间戳) */
    public long bornMs;

    /** 构造:场上豆实例(类别/位置/出生时刻) */
    public Bean(BeanType type, Point pos, long bornMs) {
        this.type = type;
        this.pos = pos;
        this.bornMs = bornMs;
    }

    /** 在场剩余寿命(负=已超时);供时限判定与存档折算 */
    public long remainingMs(long nowMs) {
        return 0L;
    }
}
