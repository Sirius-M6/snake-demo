package controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import model.ReadOnlyGameState;
import model.Snake;
import util.InMemoryStore;

/**
 * GameController 状态机/冻结语义/事件通知测试(本地自测);
 * 本批不触发 RUNNING+tick 的节拍路径(豆子链路经替身隔离),
 * 联调后补第二批:节拍推进/吃豆/穿墙/结束判定集成用例。
 */
class GameControllerTest {

    private StubBeanController beans;
    private GameController controller;

    @BeforeEach
    void setUp() {
        beans = new StubBeanController();
        // 装配:每局工厂 lambda 直接返回替身(隔离真实 BeanController,计数照常)
        controller = new GameController((state, board, events) -> beans, new InputController());
    }

    @Test
    void newGame_entersReadyWithZeroedStatus() {
        ReadOnlyGameState state = newNormalGame();
        assertEquals(GamePhase.READY, state.phase());
        assertEquals(0, state.score());
        assertEquals(0L, state.gameTimeMs());
        assertEquals(0L, state.debuffRemainingMs());
        assertEquals(SpeedConfig.initIntervalMs(Difficulty.NORMAL), state.intervalMs());
        assertEquals(1, beans.spawnInitialCalls);
    }

    @Test
    void newGame_placesSnakeAtBoardCenter() {
        ReadOnlyGameState state = newNormalGame();
        List<Point> body = state.snake().body();
        assertEquals(3, body.size());
        assertEquals(List.of(new Point(10, 10), new Point(10, 9), new Point(10, 8)), body, "头在前:头在右(10,10),开局向右");
    }

    @Test
    void tickBeforeStart_freezesGameTime() {
        ReadOnlyGameState state = newNormalGame();
        controller.tick(1_000L);
        controller.tick(2_000L);
        assertEquals(0L, state.gameTimeMs());
    }

    @Test
    void queueInput_fromReady_startsRunning() {
        newNormalGame();
        controller.queueInput(Direction.RIGHT, 1_000L);
        assertEquals(GamePhase.RUNNING, controller.state().phase());
    }

    @Test
    void pauseAndResume_onlyInLegalPhases() {
        newNormalGame();
        controller.pause();
        assertEquals(GamePhase.READY, controller.state().phase(), "READY 阶段暂停应被忽略");
        controller.queueInput(Direction.RIGHT, 1_000L);
        controller.pause();
        assertEquals(GamePhase.PAUSED, controller.state().phase());
        controller.resume();
        assertEquals(GamePhase.RUNNING, controller.state().phase());
        controller.resume();
        assertEquals(GamePhase.RUNNING, controller.state().phase(), "RUNNING 阶段恢复应被忽略");
    }

    @Test
    void tickWhilePaused_freezesGameTime() {
        newNormalGame();
        controller.queueInput(Direction.RIGHT, 1_000L);
        controller.pause();
        controller.tick(3_000L);
        controller.tick(4_000L);
        assertEquals(GamePhase.PAUSED, controller.state().phase());
        assertEquals(0L, controller.state().gameTimeMs());
    }

    @Test
    void finish_marksFinishedAndRecordsReason() {
        newNormalGame();
        controller.finish(GameOverReason.SCORE_NEGATIVE);
        assertEquals(GamePhase.FINISHED, controller.state().phase());
        assertEquals(GameOverReason.SCORE_NEGATIVE, controller.state().overReason());
    }

    @Test
    void finish_clearsDebuffAndNotifiesZero() {
        RecordingEvents events = new RecordingEvents();
        controller.registerEvents(events);
        // 预置 debuff:测试直改聚合根(吃大毒豆路径待第二批集成用例)
        ((GameState) newNormalGame()).setDebuffRemainingMs(2_000L);
        controller.finish(GameOverReason.ABANDONED);
        assertEquals(0L, controller.state().debuffRemainingMs());
        assertTrue(events.log.contains("debuff:0"));
    }

    @Test
    void restart_rebuildsFreshReadyRound() {
        newNormalGame();
        controller.finish(GameOverReason.SELF_COLLISION);
        controller.restart();
        ReadOnlyGameState state = controller.state();
        assertEquals(GamePhase.READY, state.phase());
        assertEquals(0, state.score());
        assertEquals(0L, state.gameTimeMs());
        assertEquals(2, beans.spawnInitialCalls);
    }

    @Test
    void withoutSubscriber_neverThrows() {
        assertDoesNotThrow(() -> {
            controller.newGame(fixedOptions(Difficulty.HARD));
            controller.tick(1_000L);
            controller.queueInput(Direction.RIGHT, 1_000L);
            controller.pause();
            controller.resume();
            controller.finish(GameOverReason.OBSTACLE_HIT);
            controller.restart();
        });
    }

    @Test
    void phaseEvents_recordedInOrder() {
        RecordingEvents events = new RecordingEvents();
        controller.registerEvents(events);
        newNormalGame();
        controller.queueInput(Direction.RIGHT, 1_000L);
        controller.pause();
        controller.resume();
        controller.finish(GameOverReason.ABANDONED);
        assertEquals(List.of(
                "phase:READY",
                "phase:RUNNING",
                "phase:PAUSED",
                "phase:RUNNING",
                "phase:FINISHED",
                "gameOver:ABANDONED"), events.log);
    }

    @Test
    void resumeFrom_loadsPausedSnapshotWithoutSpawn() {
        RecordingEvents events = new RecordingEvents();
        controller.registerEvents(events);
        GameState loaded = pausedSnapshot();
        controller.resumeFrom(loaded);
        assertEquals(GamePhase.PAUSED, controller.state().phase());
        assertSame(loaded, controller.state());
        assertEquals(0, beans.spawnInitialCalls, "读档装载不得补豆(豆子随存档恢复)");
        assertTrue(events.log.contains("phase:PAUSED"));
        assertTrue(events.log.contains("debuff:500"));
    }

    @Test
    void resumeFrom_thenRestart_usesLoadedSettings() {
        controller.resumeFrom(pausedSnapshot());
        controller.restart();
        ReadOnlyGameState state = controller.state();
        assertEquals(GamePhase.READY, state.phase());
        assertEquals(Difficulty.HARD, ((GameState) state).difficulty);
        assertEquals("snow", state.map().id);
        assertEquals(1, beans.spawnInitialCalls, "R 重开走 newGame:补豆一次");
    }

    @Test
    void saveTo_persistsViaSaveController() {
        SaveController saves = new SaveController(new InMemoryStore());
        newNormalGame();
        controller.saveTo(saves);
        assertTrue(saves.hasSave());
    }

    // ---------- 测试替身(嵌套类,不新增生产类) ----------

    /** 豆子调度替身:空实现 + 调用计数(隔离 B1 实现进度) */
    private static class StubBeanController extends BeanController {
        /** 替身构造:全方法覆写、不触达父类字段;占位入参传 null(B 合并后 BeanController 无无参构造) */
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

    /** 开一局默认局(中难度 + 管道) */
    private ReadOnlyGameState newNormalGame() {
        controller.newGame(fixedOptions(Difficulty.NORMAL));
        return controller.state();
    }

    /** 读档快照替身:手造 PAUSED 局面(绕过 SaveController 编码链,隔离 B2 细节) */
    private static GameState pausedSnapshot() {
        GameState loaded = new GameState();
        loaded.difficulty = Difficulty.HARD;
        loaded.map = new GameMap("snow", "雪山");
        loaded.score = 7;
        loaded.snake = new Snake(List.of(new Point(10, 9), new Point(10, 8), new Point(10, 7)));
        loaded.beans = List.of(new Bean(BeanType.SMALL, new Point(4, 4), 0L));
        loaded.debuffRemainingMs = 500L;
        loaded.setIntervalMs(150L);
        loaded.gameTimeMs = 5_000L;
        loaded.phase = GamePhase.PAUSED;
        return loaded;
    }

    /** GameOptions 替身:固定难度/地图(隔离 A2 实现进度) */
    private static GameOptions fixedOptions(Difficulty difficulty) {
        return new GameOptions() {
            @Override
            public Difficulty getDifficulty() { return difficulty; }

            @Override
            public GameMap getMap() { return new GameMap("pipe", "管道"); }
        };
    }
}