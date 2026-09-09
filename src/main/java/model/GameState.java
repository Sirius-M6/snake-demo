package model;

import java.util.List;

/**
 * 本局聚合根(implements ReadOnlyGameState;controller 唯一修改方)
 * 不含"何时刷豆/何时结束"类决策逻辑,方法只做自身状态的查询与最小变换
 */
public class GameState implements ReadOnlyGameState {

    /** 状态机与结束原因 */
    public GamePhase phase;
    public GameOverReason overReason;

    /** 本局难度与地图(来自 GameOptions) */
    public Difficulty difficulty;
    public GameMap map;

    /** 得分与蛇 */
    public int score;
    public Snake snake;

    /** 场上豆(最多 3 颗;对外只读) */
    public List<Bean> beans;

    /** 方向颠倒剩余(0 = 无) */
    public long debuffRemainingMs;

    /** 当前每格耗时(难度初始 → 提速下调 → 上限) */
    public long intervalMs;

    /** 游戏时间累计(RUNNING 才推进 → 暂停冻结的根源) */
    public long gameTimeMs;

    // ---------- 读方法(经 ReadOnlyGameState 接口对外暴露) ----------

    /** 当前得分 */
    @Override
    public int score() {
        return 0;
    }

    /** 蛇只读句柄 */
    @Override
    public Snake snake() {
        return null;
    }

    /** 场上豆只读列表 */
    @Override
    public List<Bean> beans() {
        return null;
    }

    /** 本局地图 */
    @Override
    public GameMap map() {
        return null;
    }

    /** 当前状态机阶段 */
    @Override
    public GamePhase phase() {
        return null;
    }

    /** 结束原因 */
    @Override
    public GameOverReason overReason() {
        return null;
    }

    /** 方向颠倒剩余 */
    @Override
    public long debuffRemainingMs() {
        return 0L;
    }

    /** 当前每格耗时 */
    @Override
    public long intervalMs() {
        return 0L;
    }

    /** 游戏时间累计 */
    @Override
    public long gameTimeMs() {
        return 0L;
    }

    // ---------- 变更方法(仅 controller 可调) ----------

    /** 状态迁移(controller 专用) */
    public void setPhase(GamePhase phase) {
    }

    /** 终局:置 FINISHED 与原因 */
    public void finish(GameOverReason reason) {
    }

    /** 结算分数;完成后若 < 0(分数为负)由 controller 判负 */
    public void addScore(int delta) {
    }

    /** 场上豆增(BeanController 专用) */
    public void addBean(Bean bean) {
    }

    /** 场上豆删(BeanController 专用) */
    public void removeBean(Bean bean) {
    }

    /** debuff 倒计时设定 */
    public void setDebuffRemainingMs(long remainingMs) {
    }

    /** 提速后更新每格耗时 */
    public void setIntervalMs(long intervalMs) {
    }

    /** 累计游戏时间(仅 RUNNING) */
    public void advanceGameTime(long delta) {
    }
}
