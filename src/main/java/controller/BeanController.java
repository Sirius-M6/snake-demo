package controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import config.BeanConfig;
import model.Bean;
import model.BeanType;
import model.Board;
import model.GameState;
import model.Point;
import util.WeightedPicker;

/**
 * 豆子调度状态机(B 模块);实现 BR-20/BR-25/BR-28/BR-29(PRD v1.7,2026-09-10)
 * 小豆子独立轨道:开局 1 颗、被吃后立即补 1 颗、不限时、场上恒为 1 颗;
 * 非小豆子(金/大/毒/大毒):被吃或超时消失后过 3 秒,按 BR-29 概率补刷 1 颗;
 * 补刷不记忆原空位类型(一律按抽取定类);
 * 装配:构造注入 状态/棋盘/随机源/事件(GameController 侧装配),tick 每帧被调;
 */
public class BeanController {

    /** 本局状态(GameState 读写,controller 域) */
    private final GameState state;

    /** 棋盘几何(空格枚举,障碍由 Board 内排除) */
    private final Board board;

    /** 随机源(注入 Random;AC-17/18 固定种子可复现) */
    private final WeightedPicker picker;

    /** 事件出口(onBeanRefilled 触发方为 BeanController) */
    private final GameEvents events;

    /** 非小豆子延迟补刷到期游戏时刻(可多个并存;补刷计划不入档,契约④) */
    private final List<Long> refillDueAt = new ArrayList<>();

    /** 构造:由 GameController 装配注入(公开方法签名沿用契约③,仅新增装配入口) */
    public BeanController(GameState state, Board board, WeightedPicker picker, GameEvents events) {
        this.state = state;
        this.board = board;
        this.picker = picker;
        this.events = events;
    }

    /** 开局:清空上一局遗留的补刷计划(契约④"不跨局"),再补满 3 颗——1 颗小豆子(独立轨道) + 2 颗非小豆子(按 BR-29 概率抽取);逐颗落空白格;bornMs = gameTime(0) */
    public void spawnInitial() {
        refillDueAt.clear(); // 新局开局:清空上一局遗留补刷计划(契约④;2026-09-11 集成修正)
        long now = state.gameTimeMs;
        // ① 小豆子:找空白格落位(占用 = 蛇身 + 全部豆位;障碍由 Board 内排除)
        Set<Point> occupied = new HashSet<>();
        occupied.addAll(state.snake.body());
        for (Bean bean : state.beans) {
            occupied.add(bean.pos);
        }
        List<Point> free = new ArrayList<>(board.freeCells(occupied));
        if (!free.isEmpty()) {
            Point pos = free.get(picker.nextInt(free.size()));
            Bean small = new Bean(BeanType.SMALL, pos, now);
            state.addBean(small);
            events.onBeanRefilled(small);
        }
        // ② 非小豆子 ×2:复用 spawnOne 的抽取与落位
        spawnOne();
        spawnOne();
    }

    /**
     * 小豆被吃 → 从场上移除并立即补 1 颗小豆(场上恒为 1 颗);
     * 非小豆被吃 → 移除后登记延迟补刷(refillDueAt 追加 gameTime + REFILL_DELAY_MS);
     * 移除动作在本方法完成(GameState.removeBean 为 BeanController 专用);
     * 豆子效果结算(加分/蛇身/debuff)由 GameController 负责
     */
    public void onBeanEaten(Bean bean, long gameTime) {
        state.removeBean(bean);
        if (bean.type.isSmall()) {
            // 小豆立补:找空白格落 1 颗新小豆(占用 = 蛇身 + 全部豆位)
            Set<Point> occupied = new HashSet<>();
            occupied.addAll(state.snake.body());
            for (Bean other : state.beans) {
                occupied.add(other.pos);
            }
            List<Point> free = new ArrayList<>(board.freeCells(occupied));
            if (!free.isEmpty()) {
                Point pos = free.get(picker.nextInt(free.size()));
                Bean small = new Bean(BeanType.SMALL, pos, gameTime);
                state.addBean(small);
                events.onBeanRefilled(small);
            }
        } else {
            refillDueAt.add(gameTime + BeanConfig.REFILL_DELAY_MS);
        }
    }

    /** ① 遍历场上豆:非小豆寿命 ≤ 0(负面 3s/正面 5s,小豆跳过)→ 移除并登记延迟补刷;② 到期计划(refillDueAt ≤ gameTime)逐一 spawnOne() */
    public void tick(long gameTime) {
        for (Bean bean : new ArrayList<>(state.beans)) {
            if (bean.type.isSmall()) {
                continue;
            }
            if (bean.remainingMs(gameTime) <= 0) {
                state.removeBean(bean);
                refillDueAt.add(gameTime + BeanConfig.REFILL_DELAY_MS);
            }
        }
        Iterator<Long> it = refillDueAt.iterator();
        while (it.hasNext()) {
            if (it.next() <= gameTime) {
                it.remove();
                spawnOne();
            }
        }
    }

    /**
     * 按 BR-29 概率抽取并落位 1 颗非小豆:金:大 = 1:1、毒:大毒 = 1:3、正:负 = 3:1;
     * 空格 < 10 时负面豆不参与本次抽取(仅抽金/大);受总数上限约束(含小豆 ≤ 3 颗);
     * 落位空白格(非蛇身/障碍、不与其他豆子重叠);方法名沿用架构书原名 spawnOne()
     */
    public void spawnOne() {
        // ① 防御:非小豆同时最多 2 颗(总数上限 3 含小豆)
        int nonSmall = 0;
        for (Bean b : state.beans) {
            if (!b.type.isSmall()) {
                nonSmall++;
            }
        }
        if (nonSmall >= BeanConfig.MAX_ON_BOARD - 1) {
            return;
        }
        // ② 空格枚举(占用 = 蛇身 + 全部豆位;障碍由 Board 内排除,口径与 C2 约定一致)
        Set<Point> occupied = new HashSet<>();
        occupied.addAll(state.snake.body());
        for (Bean b : state.beans) {
            occupied.add(b.pos);
        }
        List<Point> free = new ArrayList<>(board.freeCells(occupied));
        if (free.isEmpty()) {
            return; // 无空格:本次补刷作废(极端场景,不重试)
        }
        // ③ 定池:空格 < 10 仅正面池;否则 正:负 = 3:1 掷池(金:大=1:1,毒:大毒=1:3)
        boolean negative = free.size() >= BeanConfig.DEBUFF_SPAWN_FREE_CELL_LIMIT
                && picker.pick(List.of(false, true), List.of(BeanConfig.POSITIVE_TO_NEGATIVE, 1));
        List<BeanType> pool = negative
                ? List.of(BeanType.POISON, BeanType.BIG_POISON)
                : List.of(BeanType.GOLD, BeanType.BIG);
        int[] weightArr = negative ? BeanConfig.NEGATIVE_WEIGHTS : BeanConfig.POSITIVE_WEIGHTS;
        // ④ 池内按权重抽(池顺序与权重一一对应)
        List<Integer> weights = new ArrayList<>(weightArr.length);
        for (int w : weightArr) {
            weights.add(w);
        }
        BeanType type = picker.pick(pool, weights);
        // ⑤ 落位随机空格 + 入场上 + 通知 view(开局/小豆立补/到期补刷均触发;view 侧动画可选)
        Point pos = free.get(picker.nextInt(free.size()));
        Bean bean = new Bean(type, pos, state.gameTimeMs);
        state.addBean(bean);
        events.onBeanRefilled(bean);
    }
}
