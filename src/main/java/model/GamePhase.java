package model;

/**
 * 状态机枚举:READY(待开始)/RUNNING/PAUSED/FINISHED
 * 无方法;由 GameController 迁移,view 按 phase 切换界面层
 */
public enum GamePhase {

    /** 待开始 */
    READY,
    /** 运行中 */
    RUNNING,
    /** 暂停 */
    PAUSED,
    /** 已结束 */
    FINISHED;
}
