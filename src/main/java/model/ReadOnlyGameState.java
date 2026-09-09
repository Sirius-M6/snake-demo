package model;

import java.util.List;

/**
 * GameState 的只读视图接口(供 view 渲染,防呆):
 * view 在类型层面无法调用变更方法;变更方法只存在于 GameState,仅 controller 可调;
 * GameController.state() 返回本接口,view/测试一律以只读类型持有 GameState
 */
public interface ReadOnlyGameState {

    /** 当前得分 */
    int score();

    /** 蛇只读句柄 */
    Snake snake();

    /** 场上豆只读列表(最多 3 颗) */
    List<Bean> beans();

    /** 本局地图 */
    GameMap map();

    /** 当前状态机阶段 */
    GamePhase phase();

    /** 结束原因(未结束时为 null) */
    GameOverReason overReason();

    /** 方向颠倒剩余(0 = 无) */
    long debuffRemainingMs();

    /** 当前每格耗时(速度档) */
    long intervalMs();

    /** 游戏时间累计(RUNNING 才推进) */
    long gameTimeMs();
}
