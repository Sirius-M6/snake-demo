package model;

/**
 * 结束原因枚举(结算画面原因文案由 view 映射);
 * CLEARED(通关)枚举预留,判定待 QA-15
 */
public enum GameOverReason {

    /** 撞自身 */
    SELF_COLLISION,
    /** 撞障碍 */
    OBSTACLE_HIT,
    /** 分数为负 */
    SCORE_NEGATIVE,
    /** 通关(预留,判定待 QA-15) */
    CLEARED,
    /** 主动终止 */
    ABANDONED;
}
