package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.BeanConfig;
import config.MapCatalog;
import config.SpeedConfig;
import model.Bean;
import model.BeanType;
import model.Difficulty;
import model.Direction;
import model.GameMap;
import model.GameOptions;
import model.GameOverReason;
import model.GamePhase;
import model.GameState;
import model.Point;
import model.Snake;

/**
 * GameController 第二批:tick 节拍链路集成用例(RUNNING + tick 真节拍);
 * 覆盖:节拍推进/无输入直行/穿墙/障碍与自身结束/吃豆结算(小/金/毒)/debuff 倒计时/负分判负/提速;
 * 豆子调度经替身隔离(移除/补刷等 B 侧行为由 BeanControllerTest 覆盖),断言聚焦 C 侧:蛇位/分数/相位/事件/委托调用
 */
class GameControllerTickTest {

    /** 基准帧真实时刻:首帧只重建时钟基准,此后按帧距推进 */
    private static final long T0 = 1_000L;

    /** 中难度帧距(= 初始节拍,配置驱动) */
    private static final long DT = SpeedConfig.initIntervalMs(Difficulty.NORMAL);

    private StubBeanController beans;
    private GameController controller;

    @BeforeEach
    void setUp() {
        beans = new StubBeanController();
        controller = new GameController((state, board, events) -> beans, new InputController());
    }

    // ---------- 节拍与移动 ----------

    @Test
    void tickCadence_movesOneCellPerFrame() {
        GameState state = startRunning(Difficulty.NORMAL); // 蛇头 (10,10) 朝右
        controller.tick(T0 + DT);
        assertEquals(new Point(10, 11), state.snake().head(), "第 1 帧:右移一格");
        controller.tick(T0 + 2 * DT);
        assertEquals(new Point(10, 12), state.snake().head(), "第 2 帧");
        controller.tick(T0 + 3 * DT);
        assertEquals(new Point(10, 13), state.snake().head(), "第 3 帧");
        assertEquals(List.of(new Point(10, 13), new Point(10, 12), new Point(10, 11)), state.snake().body());
        assertEquals(3, beans.tickCalls, "每帧委托豆子调度一次");
    }

    @Test
    void runningFirstBeat_withoutInput_keepsHeading() {
        controller.newGame(fixedOptions(Difficulty.NORMAL));
        GameState state = (GameState) controller.state();
        state.setPhase(GamePhase.RUNNING); // 直启/读档继续场景:无任何方向输入历史
        controller.tick(T0);
        controller.tick(T0 + DT);
        assertEquals(new Point(10, 11), state.snake().head(), "无输入且无历史:保持头方向直行(不得抛 NPE)");
    }

    @Test
    void wrap_acrossRightEdge_reappearsAtLeft() {
        GameState state = startRunning(Difficulty.NORMAL);
        state.snake = new Snake(List.of(new Point(10, 19), new Point(10, 18), new Point(10, 17)));
        controller.tick(T0 + DT);
        assertEquals(new Point(10, 0), state.snake().head(), "右缘穿墙 → 列 0");
        assertEquals(List.of(new Point(10, 0), new Point(10, 19), new Point(10, 18)), state.snake().body());
    }

    // ---------- 结束判定 ----------

    @Test
    void obstacleAhead_finishesWithObstacleHit() {
        RecordingEvents events = new RecordingEvents();
        controller.registerEvents(events);
        GameMap pipe = MapCatalog.byId("pipe");
        controller.newGame(new GameOptions() {
            @Override
            public Difficulty getDifficulty() { return Difficulty.NORMAL; }

            @Override
            public GameMap getMap() { return pipe; }
        });
        GameState state = (GameState) controller.state();
        state.snake = new Snake(List.of(new Point(1, 10), new Point(2, 10), new Point(3, 10))); // 头朝上,上方 (0,10) 是墙
        controller.queueInput(Direction.UP, T0);
        controller.tick(T0);
        controller.tick(T0 + DT);
        assertEquals(GamePhase.FINISHED, state.phase());
        assertEquals(GameOverReason.OBSTACLE_HIT, state.overReason());
        assertTrue(events.log.contains("gameOver:OBSTACLE_HIT"));
    }

    @Test
    void selfCollision_finishesWithSelfCollision() {
        RecordingEvents events = new RecordingEvents();
        controller.registerEvents(events);
        GameState state = startRunning(Difficulty.NORMAL);
        // U 形 6 节:头 (5,5) 朝左;下方 (6,5) 是尾节 —— 转向 DOWN 后下一步踩到尾
        state.snake = new Snake(List.of(
                new Point(5, 5), new Point(5, 6), new Point(5, 7),
                new Point(6, 7), new Point(6, 6), new Point(6, 5)));
        controller.queueInput(Direction.DOWN, T0 + DT);
        controller.tick(T0 + DT);
        assertEquals(GamePhase.FINISHED, state.phase());
        assertEquals(GameOverReason.SELF_COLLISION, state.overReason());
        assertTrue(events.log.contains("gameOver:SELF_COLLISION"));
    }

    // ---------- 吃豆结算 ----------

    @Test
    void eatSmall_scoresAndGrows() {
        GameState state = startRunning(Difficulty.NORMAL);
        RecordingEvents events = new RecordingEvents();
        controller.registerEvents(events);
        state.addBean(new Bean(BeanType.SMALL, new Point(10, 11), 0L));
        controller.tick(T0 + DT);
        assertEquals(1, state.score());
        assertEquals(4, state.snake().length(), "小豆:尾 +1 → 3+1=4");
        assertEquals(1, beans.beanEatenCalls, "吃豆委托 BeanController.onBeanEaten");
        assertTrue(events.log.contains("beanEaten:SMALL"));
    }

    @Test
    void eatGold_shrinksTwoCells() {
        GameState state = startRunning(Difficulty.NORMAL);
        state.snake = new Snake(List.of(
                new Point(10, 13), new Point(10, 12), new Point(10, 11),
                new Point(10, 10), new Point(10, 9))); // 5 节,头朝右
        state.addBean(new Bean(BeanType.GOLD, new Point(10, 14), 0L));
        controller.tick(T0 + DT);
        assertEquals(6, state.score());
        assertEquals(3, state.snake().length(), "金豆:固定消除 2 节 → 5-2=3");
        assertEquals(new Point(10, 14), state.snake().head());
    }

    @Test
    void eatBigPoison_setsAndResetsDebuff() {
        GameState state = startRunning(Difficulty.NORMAL);
        RecordingEvents events = new RecordingEvents();
        controller.registerEvents(events);
        state.addScore(20); // 预置分数:避免 0-10 扣至负分先行判负
        state.addBean(new Bean(BeanType.BIG_POISON, new Point(10, 11), 0L));
        state.addBean(new Bean(BeanType.BIG_POISON, new Point(10, 12), 0L));
        controller.tick(T0 + DT);
        assertEquals(10, state.score());
        assertEquals(BeanConfig.DEBUFF_MS - DT, state.debuffRemainingMs(), "吃大毒:debuff 置 2s(同帧再递减一帧量)");
        controller.tick(T0 + 2 * DT);
        assertEquals(BeanConfig.DEBUFF_MS - DT, state.debuffRemainingMs(), "再吃重置:重新计 2s 而非叠加");
        assertEquals(2, Collections.frequency(events.log, "debuff:2000"), "两次吃毒各通知一次 debuff:2000");
    }

    @Test
    void debuffCountdown_clearsWithZeroEvent() {
        GameState state = startRunning(Difficulty.NORMAL);
        RecordingEvents events = new RecordingEvents();
        controller.registerEvents(events);
        state.setDebuffRemainingMs(2 * DT); // 300ms:两帧后清零
        controller.tick(T0 + DT);
        assertEquals(DT, state.debuffRemainingMs(), "一帧后递减一帧量");
        controller.tick(T0 + 2 * DT);
        assertEquals(0L, state.debuffRemainingMs());
        assertTrue(events.log.contains("debuff:0"), "清零帧通知 debuff:0");
    }

    @Test
    void scoreNegative_afterPoison_finishes() {
        GameState state = startRunning(Difficulty.NORMAL); // score 0
        RecordingEvents events = new RecordingEvents();
        controller.registerEvents(events);
        state.addBean(new Bean(BeanType.BIG_POISON, new Point(10, 11), 0L));
        controller.tick(T0 + DT);
        assertEquals(-10, state.score());
        assertEquals(GamePhase.FINISHED, state.phase());
        assertEquals(GameOverReason.SCORE_NEGATIVE, state.overReason(), "扣至负分:判负");
        assertTrue(events.log.contains("gameOver:SCORE_NEGATIVE"));
    }

    // ---------- 提速 ----------

    @Test
    void speedUp_whenScoreReachesTier() {
        GameState state = startRunning(Difficulty.EASY); // 初始 200ms
        state.addScore(10); // 达 10 分档
        long easyDt = SpeedConfig.initIntervalMs(Difficulty.EASY);
        controller.tick(T0 + easyDt);
        assertEquals(180L, state.intervalMs(), "达 10 分档:200 → 180 下调");
    }

    // ---------- 测试替身(嵌套类,不新增生产类) ----------

    /** 豆子调度替身:空实现 + 调用计数(隔离 B 侧行为,聚焦 C 侧委托与事件) */
    private static class StubBeanController extends BeanController {
        /** 替身构造:全方法覆写、不触达父类字段;占位入参传 null */
        StubBeanController() {
            super(null, null, null, null);
        }

        int spawnInitialCalls;
        int beanEatenCalls;
        int tickCalls;

        @Override
        public void spawnInitial() { spawnInitialCalls++; }

        @Override
        public void onBeanEaten(Bean bean, long gameTime) { beanEatenCalls++; }

        @Override
        public void tick(long gameTime) { tickCalls++; }
    }

    /** 事件记录替身:按调用顺序记录为字符串 */
    private static class RecordingEvents implements GameEvents {
        final List<String> log = new ArrayList<>();

        @Override
        public void onBeanEaten(BeanType type, Point pos) { log.add("beanEaten:" + type); }

        @Override
        public void onBeanRefilled(Bean bean) { log.add("beanRefilled"); }

        @Override
        public void onDebuffChanged(long remainingMs) { log.add("debuff:" + remainingMs); }

        @Override
        public void onGameOver(GameOverReason reason) { log.add("gameOver:" + reason); }

        @Override
        public void onPhaseChanged(GamePhase phase) { log.add("phase:" + phase); }
    }

    // ---------- 辅助 ----------

    /** 开一局(固定难度+空图)并转 RUNNING;首帧只重建时钟基准,T0 后按帧距自行推进 */
    private GameState startRunning(Difficulty difficulty) {
        controller.newGame(fixedOptions(difficulty));
        controller.queueInput(Direction.RIGHT, T0); // READY → RUNNING
        controller.tick(T0); // 基准帧:不推进
        return (GameState) controller.state();
    }

    /** GameOptions 替身:固定难度 + 空管道图(无障碍网格,隔离地图数据) */
    private static GameOptions fixedOptions(Difficulty difficulty) {
        return new GameOptions() {
            @Override
            public Difficulty getDifficulty() { return difficulty; }

            @Override
            public GameMap getMap() { return new GameMap("pipe", "管道"); }
        };
    }
}
