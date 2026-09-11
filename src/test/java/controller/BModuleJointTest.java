package controller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import config.BeanConfig;
import config.SaveConfig;
import model.Bean;
import model.BeanType;
import model.Board;
import model.Difficulty;
import model.GameMap;
import model.GameOverReason;
import model.GamePhase;
import model.GameState;
import model.Point;
import model.SaveData;
import model.Snake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.InMemoryStore;
import util.TextCodec;
import util.WeightedPicker;

/**
 * B1×B2 对接测试:BeanController 产出的实况局面 → SaveController 存档链路;
 * 锁定契约④(豆存剩余寿命、补刷计划不入档)与读档折算公式的可逆性;
 * 读档成功路径待 A2 实现 MapCatalog.byId、C2 实现 Snake 后启用(与 SaveControllerTest 同阻塞项)
 */
class BModuleJointTest {

    /** 内存存储(不碰磁盘) */
    private final InMemoryStore store = new InMemoryStore();

    /** B2 被测对象 */
    private final SaveController saves = new SaveController(store);

    /** 编解码(断言落盘文本用) */
    private final TextCodec codec = new TextCodec();

    // ===== 存档链路:B1 实况 → B2 落盘 =====

    @Test
    @DisplayName("存:第 4 列落各豆剩余寿命(非小豆=时限−在场),小豆哨兵 MAX")
    void saveAfterPlayStoresRemainingLifetimes() {
        GameState state = newState();
        BeanController beans = newBeanController(state);
        beans.spawnInitial(); // 开局 3 颗 @0

        Bean small = firstBean(state, true);
        beans.onBeanEaten(small, 2000L); // 小豆立补 @2000
        state.gameTimeMs = 2000L;

        saves.saveNow(state);
        SaveData back = codec.decodeSave(store.loadText(savePath()).orElseThrow());

        assertNotNull(back);
        assertEquals(3, back.beans.size());
        // 逐颗核对:存档第 4 列应等于存档时刻的剩余寿命(而非出生时刻)
        Map<Long, Bean> live = new HashMap<>();
        for (Bean b : state.beans) {
            live.put(keyOf(b.pos), b);
        }
        for (Bean saved : back.beans) {
            Bean origin = live.get(keyOf(saved.pos));
            assertNotNull(origin, "存档豆位置应与场上一致: " + saved.type);
            assertEquals(origin.type, saved.type);
            assertEquals(origin.remainingMs(2000L), saved.bornMs, "第 4 列应存剩余寿命: " + saved.type);
        }
        // 小豆:不限时哨兵直通
        Bean savedSmall = back.beans.stream().filter(b -> b.type == BeanType.SMALL).findFirst().orElseThrow();
        assertEquals(Long.MAX_VALUE, savedSmall.bornMs);
    }

    @Test
    @DisplayName("存:补刷计划不入档,场上剩 2 颗即只存 2 行")
    void refillPlanNotSaved() {
        GameState state = newState();
        BeanController beans = newBeanController(state);
        beans.spawnInitial();

        Bean nonSmall = firstBean(state, false);
        beans.onBeanEaten(nonSmall, 500L); // 登记延迟补刷(计划不入档)
        state.gameTimeMs = 500L;

        saves.saveNow(state);
        String text = store.loadText(savePath()).orElseThrow();
        SaveData back = codec.decodeSave(text);

        assertNotNull(back);
        assertEquals(2, back.beans.size());
        assertEquals(2, text.lines().filter(l -> l.startsWith("bean=")).count());
        Set<Long> savedKeys = new HashSet<>();
        for (Bean b : back.beans) {
            savedKeys.add(keyOf(b.pos));
        }
        for (Bean b : state.beans) {
            assertTrue(savedKeys.contains(keyOf(b.pos)), "场上豆应在档: " + b.type);
        }
    }

    // ===== 读档折算公式(与 SaveController.loadAndResume 同式) =====

    @Test
    @DisplayName("折算可逆:bornMs = 存档时刻 + 剩余寿命 − 时限 ⇒ 恢复后剩余不变")
    void loadFormulaInvertsRemaining() {
        long gameTimeMs = 123_456L;
        for (BeanType type : BeanType.values()) {
            long lifespan = BeanConfig.duration(type);
            if (lifespan == Long.MAX_VALUE) {
                // 小豆:哨兵语义 —— 折算后 bornMs=存档时刻,恢复后仍恒为 MAX
                Bean restored = new Bean(type, new Point(1, 1), gameTimeMs);
                assertEquals(Long.MAX_VALUE, restored.remainingMs(gameTimeMs));
            } else {
                for (long saved : new long[] {1L, 500L, lifespan}) {
                    long bornMs = gameTimeMs + saved - lifespan; // 与 loadAndResume 同步
                    Bean restored = new Bean(type, new Point(1, 1), bornMs);
                    assertEquals(saved, restored.remainingMs(gameTimeMs), "类型 " + type + " 折算应可逆");
                }
            }
        }
    }

    // ===== 读档成功路径 =====

    @Test
    @DisplayName("读:实况存档恢复后,各豆剩余寿命与存档一致、小豆哨兵保持")
    void loadRestoresBeansRemaining() {
        GameState state = newState();
        BeanController beans = newBeanController(state);
        beans.spawnInitial();
        beans.onBeanEaten(firstBean(state, true), 2000L);
        state.gameTimeMs = 2000L;
        saves.saveNow(state);
        SaveData back = codec.decodeSave(store.loadText(savePath()).orElseThrow());
        Map<Long, Long> savedRemaining = new HashMap<>();
        for (Bean b : back.beans) {
            savedRemaining.put(keyOf(b.pos), b.bornMs);
        }

        Optional<GameState> loaded = saves.loadAndResume();

        assertTrue(loaded.isPresent());
        GameState restored = loaded.get();
        assertEquals(GamePhase.PAUSED, restored.phase);
        assertEquals(2000L, restored.gameTimeMs);
        assertEquals(3, restored.beans.size());
        for (Bean b : restored.beans) {
            assertEquals(savedRemaining.get(keyOf(b.pos)).longValue(), b.remainingMs(2000L),
                    "恢复后剩余寿命应不变: " + b.type);
        }
    }

    // ===== 辅助 =====

    /** 最小可存档局面(pipe 图,含蛇身替身) */
    private GameState newState() {
        GameState state = new GameState();
        state.map = new GameMap("pipe", "测试图");
        state.difficulty = Difficulty.NORMAL;
        state.intervalMs = 150L;
        state.snake = new StubSnake(new ArrayList<>(List.of(new Point(10, 8), new Point(10, 9), new Point(10, 10))));
        return state;
    }

    /** B1 被测对象:全图空格 + 固定种子 */
    private BeanController newBeanController(GameState state) {
        return new BeanController(state, new StubBoard(fullCells()), new WeightedPicker(new Random(7L)), new NoopEvents());
    }

    /** 第一颗小豆(small=true)或非小豆(small=false) */
    private static Bean firstBean(GameState state, boolean small) {
        return state.beans.stream().filter(b -> b.type.isSmall() == small).findFirst().orElseThrow();
    }

    /** 存档文件路径(与 SaveController 内部一致) */
    private String savePath() {
        return SaveConfig.SAVE_DIR + File.separator + SaveConfig.SAVE_FILE;
    }

    /** 坐标键值(row*1000+col;Point.equals 为 identity,测试一律按键值比较) */
    private static long keyOf(Point p) {
        return p.row * 1000L + p.col;
    }

    /** 20×20 全图空格 */
    private static List<Point> fullCells() {
        List<Point> cells = new ArrayList<>();
        for (int r = 0; r < 20; r++) {
            for (int c = 0; c < 20; c++) {
                cells.add(new Point(r, c));
            }
        }
        return cells;
    }

    // ===== 测试替身(C2 骨架实现前) =====

    /** 蛇身替身:仅 body() 供存档读取 */
    private static final class StubSnake extends Snake {

        private final List<Point> cells;

        StubSnake(List<Point> cells) {
            super(cells);
            this.cells = cells;
        }

        @Override
        public List<Point> body() {
            return cells;
        }
    }

    /** 棋盘替身:固定空格池,按占用键值过滤 */
    private static final class StubBoard extends Board {

        private final List<Point> pool;

        StubBoard(List<Point> pool) {
            super(20, 20, Set.of());
            this.pool = pool;
        }

        @Override
        public Set<Point> freeCells(Set<Point> occupied) {
            Set<Long> busy = new HashSet<>();
            for (Point p : occupied) {
                busy.add(keyOf(p));
            }
            Set<Point> free = new LinkedHashSet<>();
            for (Point p : pool) {
                if (!busy.contains(keyOf(p))) {
                    free.add(p);
                }
            }
            return free;
        }
    }

    /** 事件替身:全部空实现 */
    private static final class NoopEvents implements GameEvents {

        @Override
        public void onBeanEaten(BeanType type, Point pos) {
        }

        @Override
        public void onBeanRefilled(Bean bean) {
        }

        @Override
        public void onDebuffChanged(long remainingMs) {
        }

        @Override
        public void onGameOver(GameOverReason reason) {
        }

        @Override
        public void onPhaseChanged(GamePhase phase) {
        }
    }
}
