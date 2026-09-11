package model;

import config.BeanConfig;

/**
 * 场上豆实例:类型/位置/出生时刻
 * 时限数值唯一来源:config.BeanConfig(model→config 引用随实现备案,BR-28)
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

    /** 在场剩余寿命(负=已超时);正面豆 5 秒/负面豆 3 秒/小豆不限时(BR-28);供时限判定与存档折算 */
    public long remainingMs(long nowMs) {
        long lifespan = BeanConfig.duration(type);
        if (lifespan == Long.MAX_VALUE) {
            return Long.MAX_VALUE; // 小豆不限时:哨兵值(存档语义同义,与 B2 对齐)
        }
        return bornMs + lifespan - nowMs;
    }
}
