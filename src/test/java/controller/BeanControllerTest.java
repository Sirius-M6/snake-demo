package controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import config.BeanConfig;
import model.Bean;
import model.BeanType;
import model.Board;
import model.GameOverReason;
import model.GamePhase;
import model.GameState;
import model.Point;
import model.Snake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.WeightedPicker;

/**
 * BeanController 单元测试:B1 调度规则 BR-25/28/29(v1.7);
 * C2 骨架(Snake/Board)实现前,以内嵌替身 StubSnake/StubBoard 注入;
 * 固定种子随机源 → 抽取比例统计可复现(AC-17/18)
 */
class BeanControllerTest {

    /** 全图空格池(20×20,值坐标) */
    private static final List<Point> FULL_CELLS = fullCells();

    /** 蛇身初始 3 节(棋盘中央) */
    private static final List<Point> SNAKE_CELLS = List.of(new Point(10, 8), new Point(10, 9), new Point(10, 10));

    /** 本局状态 */
    private GameState state;

    /** 被测对象(默认:全图空格 + 固定种子) */
    private BeanController controller;

    /** 事件替身(统计补刷通知) */
    private RecordingEvents events;

    @BeforeEach
    void setUp() {
        state = new GameState();
        state.snake = new StubSnake(new ArrayList<>(SNAKE_CELLS));
        state.gameTimeMs = 0L;
        events = new RecordingEvents();
        useBoard(new StubBoard(FULL_CELLS));
    }

    // ===== 开局铺场 =====

    @Test
    @DisplayName("开局:1 颗小豆 + 2 颗非小豆;落位不压蛇身、互不重叠、bornMs=开局时刻")
    void spawnInitialFillsOneSmallAndTwoNonSmall() {
        controller.spawnInitial();

        assertEquals(3, state.beans.size());
        assertEquals(1, smallCount());
        assertEquals(2, nonSmallCount());
        // 落位:互不重叠、不压蛇身、均在棋盘内
        Set<Long> occupied = keys(positionsOf(state.beans));
        assertEquals(3, occupied.size());
        assertTrue(Collections.disjoint(occupied, keys(SNAKE_CELLS)));
        Set<Long> boardKeys = keys(FULL_CELLS);
        for (Bean b : state.beans) {
            assertTrue(boardKeys.contains(keyOf(b.pos)));
            assertEquals(0L, b.bornMs);
        }
        assertEquals(3, events.refilled); // 逐颗通知 view
    }

    // ===== 小豆独立轨道 =====

    @Test
    @DisplayName("小豆被吃:立即补 1 颗(bornMs=吃时 gameTime),场上恒 1 颗且不重叠")
    void smallEatenRefillsImmediately() {
        controller.spawnInitial();
        Bean small = firstOf(true);
        controller.onBeanEaten(small, 2000L);

        assertEquals(3, state.beans.size());
        assertEquals(1, smallCount());
        Bean refilled = firstOf(true);
        assertEquals(2000L, refilled.bornMs);
        Set<Long> occupied = keys(positionsOf(state.beans));
        assertEquals(3, occupied.size());
        assertTrue(Collections.disjoint(occupied, keys(SNAKE_CELLS)));
        assertEquals(4, events.refilled);
    }

    // ===== 非小豆延迟补刷(BR-25) =====

    @Test
    @DisplayName("非小豆被吃:3 秒延迟补刷(3999 不刷,4000 刷)")
    void nonSmallEatenRefillsAfterDelay() {
        state.addBean(new Bean(BeanType.SMALL, new Point(3, 3), 0L));
        Bean gold = new Bean(BeanType.GOLD, new Point(4, 4), 0L);
        state.addBean(gold);
        controller.onBeanEaten(gold, 1000L); // 计划到期 = 1000 + REFILL_DELAY_MS

        long due = 1000L + BeanConfig.REFILL_DELAY_MS;
        tickAt(due - 1L);
        assertEquals(0, nonSmallCount());
        assertEquals(0, events.refilled);

        tickAt(due);
        assertEquals(1, nonSmallCount());
        assertEquals(due, firstOf(false).bornMs);
        assertEquals(1, events.refilled);
    }

    @Test
    @DisplayName("非小豆超时:毒 3000 消失→6000 补;大 5000 消失→8000 补(小豆不受时限)")
    void nonSmallTimeoutClearsAndRefills() {
        state.addBean(new Bean(BeanType.SMALL, new Point(3, 3), 0L));
        state.addBean(new Bean(BeanType.POISON, new Point(6, 6), 0L));
        state.addBean(new Bean(BeanType.BIG, new Point(7, 7), 0L));
        long poisonDue = BeanConfig.NEGATIVE_LIFETIME_MS + BeanConfig.REFILL_DELAY_MS;
        long bigDue = BeanConfig.POSITIVE_LIFETIME_MS + BeanConfig.REFILL_DELAY_MS;

        tickAt(BeanConfig.NEGATIVE_LIFETIME_MS); // 毒剩余 0 → 消失,登记补刷
        assertEquals(2, state.beans.size());
        assertEquals(BeanType.BIG, firstOf(false).type);
        assertEquals(0, events.refilled);

        tickAt(BeanConfig.POSITIVE_LIFETIME_MS); // 大剩余 0 → 消失,登记补刷
        assertEquals(1, state.beans.size());

        tickAt(poisonDue - 1L);
        assertEquals(0, nonSmallCount());
        tickAt(poisonDue); // 毒豆补刷到点
        assertEquals(1, nonSmallCount());
        assertEquals(poisonDue, firstOf(false).bornMs);

        tickAt(bigDue - 1L);
        assertEquals(1, nonSmallCount());
        tickAt(bigDue); // 大豆补刷到点
        assertEquals(2, nonSmallCount());
        assertEquals(2, events.refilled);
    }

    // ===== 上限与空格约束(BR-28/29) =====

    @Test
    @DisplayName("非小豆上限:已 2 颗时 spawnOne 不再补;释放名额后可再补")
    void nonSmallCapAtTwo() {
        state.addBean(new Bean(BeanType.SMALL, new Point(3, 3), 0L));
        Bean gold = new Bean(BeanType.GOLD, new Point(4, 4), 0L);
        Bean poison = new Bean(BeanType.POISON, new Point(5, 5), 0L);
        state.addBean(gold);
        state.addBean(poison);

        controller.spawnOne(); // 上限拦截
        assertEquals(3, state.beans.size());
        assertEquals(0, events.refilled);

        state.removeBean(poison); // 手动释放名额(不走 onBeanEaten,避免引入补刷计划)
        controller.spawnOne();
        assertEquals(3, state.beans.size());
        assertEquals(2, nonSmallCount());
        assertEquals(1, events.refilled);
    }

    @Test
    @DisplayName("无空格:小豆被吃不补、spawnOne 不落新豆(极端场景不重试)")
    void noFreeCellNoRefill() {
        Point a = new Point(0, 0);
        Point b = new Point(0, 1);
        state.snake = new StubSnake(new ArrayList<>(List.of(a, b)));
        useBoard(new StubBoard(List.of(a, b))); // 全图仅 2 格,恰被蛇身占满
        Bean small = new Bean(BeanType.SMALL, new Point(9, 9), 0L);
        state.addBean(small);

        controller.onBeanEaten(small, 1000L);
        assertTrue(state.beans.isEmpty());
        assertEquals(0, events.refilled);

        controller.spawnOne();
        assertTrue(state.beans.isEmpty());
    }

    @Test
    @DisplayName("空格 < 10:负面豆不参与抽取,连续 50 次仅出正面豆")
    void underTenFreeCellsOnlyPositive() {
        List<Point> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nine.add(new Point(i, 0));
        }
        state.snake = new StubSnake(new ArrayList<>()); // 不占格,空格恰 9
        useBoard(new StubBoard(nine));
        Set<Long> poolKeys = keys(nine);

        for (int i = 0; i < 50; i++) {
            clearBeans();
            controller.spawnOne();
            assertEquals(1, state.beans.size());
            assertTrue(state.beans.get(0).type.isPositive(),
                    "空格 <10 不应抽到负面豆: " + state.beans.get(0).type);
            assertTrue(poolKeys.contains(keyOf(state.beans.get(0).pos)));
        }
    }

    // ===== 抽取比例(固定种子,AC-17/18) =====

    @Test
    @DisplayName("固定种子 1200 次:正:负≈3:1、金:大≈1:1、毒:大毒≈1:3")
    void ratioSamplingUnderFixedSeed() {
        int gold = 0;
        int big = 0;
        int poison = 0;
        int bigPoison = 0;
        int n = 1200;
        for (int i = 0; i < n; i++) {
            clearBeans();
            controller.spawnOne();
            switch (state.beans.get(0).type) {
                case GOLD -> gold++;
                case BIG -> big++;
                case POISON -> poison++;
                case BIG_POISON -> bigPoison++;
                default -> fail("spawnOne 不应产出小豆");
            }
        }
        int positive = gold + big;
        int negative = poison + bigPoison;
        assertEquals(n, positive + negative);
        // 期望:正面 900(±8σ,σ≈15)
        assertTrue(positive >= 780 && positive <= 1020, "正:负≈3:1 偏离过大, 正面=" + positive);
        // 期望:金 450(占正面 1/2)
        assertTrue(gold >= positive * 0.4 && gold <= positive * 0.6, "金:大≈1:1 偏离过大, 金=" + gold);
        // 期望:毒 75(占负面 1/4)
        assertTrue(poison >= negative * 0.15 && poison <= negative * 0.35, "毒:大毒≈1:3 偏离过大, 毒=" + poison);
    }

    // ===== 跨局防护(契约④:补刷计划不入档、不跨局) =====

    @Test
    @DisplayName("spawnInitial 清空上一局遗留的补刷计划")
    void spawnInitialClearsStaleRefillPlan() {
        Bean gold = new Bean(BeanType.GOLD, new Point(3, 3), 0L);
        state.addBean(gold);
        controller.onBeanEaten(gold, 1000L); // 旧局留下 4000 到期的计划

        state.gameTimeMs = 0L; // 新局:时钟归零
        controller.spawnInitial();
        assertEquals(3, state.beans.size());
        for (Bean b : new ArrayList<>(state.beans)) {
            if (!b.type.isSmall()) {
                state.removeBean(b); // 清走非小豆:若旧计划未清,4000 tick 会补出一颗
            }
        }
        assertEquals(0, nonSmallCount());

        tickAt(3999L);
        tickAt(4000L);
        assertEquals(0, nonSmallCount(), "旧局补刷计划不应在新局触发");
        assertEquals(3, events.refilled); // 仅 spawnInitial 的 3 次补刷,之后无新增
    }

    // ===== 辅助 =====

    /** 状态时钟拨到 t 再 tick(生产:GameController 每帧先推进 gameTimeMs 再调 tick) */
    private void tickAt(long t) {
        state.gameTimeMs = t;
        controller.tick(t);
    }

    /** 重装配:替换棋盘,其余依赖保持(固定种子) */
    private void useBoard(Board board) {
        controller = new BeanController(state, board, new WeightedPicker(new Random(42L)), events);
    }

    /** 清空场上豆 */
    private void clearBeans() {
        for (Bean b : new ArrayList<>(state.beans)) {
            state.removeBean(b);
        }
    }

    /** 小豆数 */
    private int smallCount() {
        int count = 0;
        for (Bean b : state.beans) {
            if (b.type.isSmall()) {
                count++;
            }
        }
        return count;
    }

    /** 非小豆数 */
    private int nonSmallCount() {
        int count = 0;
        for (Bean b : state.beans) {
            if (!b.type.isSmall()) {
                count++;
            }
        }
        return count;
    }

    /** 第一颗小豆(small=true)或非小豆(small=false) */
    private Bean firstOf(boolean small) {
        return state.beans.stream().filter(b -> b.type.isSmall() == small).findFirst().orElseThrow();
    }

    /** 豆位列表 */
    private static List<Point> positionsOf(List<Bean> beans) {
        List<Point> positions = new ArrayList<>();
        for (Bean b : beans) {
            positions.add(b.pos);
        }
        return positions;
    }

    /** 坐标键值(row*1000+col;Point.equals 为 identity,测试一律按键值比较) */
    private static long keyOf(Point p) {
        return p.row * 1000L + p.col;
    }

    /** 坐标集合 → 键值集合 */
    private static Set<Long> keys(Collection<Point> points) {
        Set<Long> keys = new HashSet<>();
        for (Point p : points) {
            keys.add(keyOf(p));
        }
        return keys;
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

    /** 蛇身替身:仅 body() 供占位枚举 */
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

    /** 棋盘替身:固定空格池,按占用键值过滤;LinkedHashSet 保持池序 → 固定种子下抽取可复现 */
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

        @Override
        public int countFree(Set<Point> occupied) {
            return freeCells(occupied).size();
        }
    }

    /** 事件替身:仅统计补刷通知 */
    private static final class RecordingEvents implements GameEvents {

        int refilled;

        @Override
        public void onBeanRefilled(Bean bean) {
            refilled++;
        }

        @Override
        public void onBeanEaten(BeanType type, Point pos) {
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
