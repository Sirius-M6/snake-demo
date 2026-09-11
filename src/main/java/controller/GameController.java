package controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import config.BeanConfig;
import config.BoardConfig;
import config.MapCatalog;
import config.SpeedConfig;
import model.Bean;
import model.BeanType;
import model.Board;
import model.Direction;
import model.GameMap;
import model.GameOptions;
import model.GameOverReason;
import model.GamePhase;
import model.GameState;
import model.Point;
import model.ReadOnlyGameState;
import model.Snake;

/**
 * 游戏主控制器(C 模块):主循环与状态机 —— 推进/碰撞/结算/暂停/提速;
 * view 每帧只调 tick()
 */
public class GameController {

    /** 下一移动节拍的游戏时刻(初始 0,之后 += intervalMs) */
    private long nextMoveAtMs;

    /** 上帧真实时钟,用于累计游戏时间 */
    private long lastRealMs;

    /** 本局运行状态(controller 唯一变更方) */
    private GameState gameState;

    /** 本局选项(restart 以同难度/地图重开;newGame 时更新) */
    private GameOptions options;

    /** 豆子调度(B 模块):每局新建,与一局同生共死(方式1:跨局状态天然清零) */
    private BeanController beanController;

    /** 豆子调度按局工厂(装配注入;newGame 每局调一次) */
    private final BeanControllerFactory beanFactory;

    /** 输入判定(C 模块,装配时注入) */
    private final InputController inputController;

    /** 事件回调(view 经 registerEvents 订阅;未订阅时为 null,触发点判空) */
    private GameEvents events;

    /** 事件转发壳:BeanController 创建先于 view 订阅(§9.1 时序),一律转发到"当前"events 字段 */
    private final GameEvents beanEvents = new GameEvents() {
        @Override
        public void onBeanEaten(BeanType type, Point pos) {
            if (events != null) {
                events.onBeanEaten(type, pos);
            }
        }

        @Override
        public void onBeanRefilled(Bean bean) {
            if (events != null) {
                events.onBeanRefilled(bean);
            }
        }

        @Override
        public void onDebuffChanged(long remainingMs) {
            if (events != null) {
                events.onDebuffChanged(remainingMs);
            }
        }

        @Override
        public void onGameOver(GameOverReason reason) {
            if (events != null) {
                events.onGameOver(reason);
            }
        }

        @Override
        public void onPhaseChanged(GamePhase phase) {
            if (events != null) {
                events.onPhaseChanged(phase);
            }
        }
    };

    /** 装配:MainApp 注入豆子调度工厂与输入判定(BeanController 由工厂按局新建) */
    public GameController(BeanControllerFactory beanFactory, InputController inputController) {
        this.beanFactory = beanFactory;
        this.inputController = inputController;
    }

    /**
     * 豆子调度按局工厂(方式1 装配口):B 的 BeanController 构造时即锁定 state/board,
     * 而 GameState 每局由 newGame 重建 —— 故每局经本工厂新建,实例与一局同生共死;
     * 嵌套于本类:不新增 .java 文件,零合并面(工厂实现由 MainApp/测试的 lambda 提供)
     */
    @FunctionalInterface
    public interface BeanControllerFactory {

        /** 造出绑定本局 state/board/events 的豆子调度 */
        BeanController create(GameState state, Board board, GameEvents events);
    }

    /** 建局:按难度取 intervalMs;蛇置中;BeanController.spawnInitial();phase=READY;通知事件 */
    public void newGame(GameOptions options) {
        this.options = options;

        GameState state = new GameState();
        state.difficulty = options.getDifficulty();
        // 地图兜底:未选图(主菜单直接开局)取默认第一张(与地图页"空 = 默认第一张"口径一致),避免碰撞判定撞 null
        GameMap map = options.getMap();
        state.map = map != null ? map : MapCatalog.defaultMaps().get(0);
        state.snake = new Snake(BoardConfig.snakeInit());
        state.setIntervalMs(SpeedConfig.initIntervalMs(options.getDifficulty()));
        state.setPhase(GamePhase.READY);
        gameState = state;

        nextMoveAtMs = 0;
        lastRealMs = 0; // 时钟基准置零:下一帧 tick 重建基准,避免跨局 Δ 误计
        beanController = beanFactory.create(gameState, buildBoard(state.map), beanEvents); // 每局新建:绑定本局 state/board/events
        beanController.spawnInitial();

        if (events != null) {
            events.onPhaseChanged(GamePhase.READY);
        }
    }

    /**
     * 装载读档重建的局面(继续游戏;restored 应为 PAUSED 快照):
     * 时钟基准置零(下一帧 tick 重建,避免把读档前的真实时间差计入);
     * 豆子调度按局重建但不 spawnInitial —— 场上豆子已随存档恢复(补刷计划不入档);
     * 选项同步读档局的难度/地图(供 R 重开沿用);已有订阅者时通知 PAUSED(弹暂停层)
     */
    public void resumeFrom(GameState restored) {
        if (restored == null) {
            return;
        }
        restored.setPhase(GamePhase.PAUSED);
        gameState = restored;
        if (options == null) {
            options = new GameOptions();
        }
        options.setDifficulty(restored.difficulty);
        options.setMap(restored.map);
        nextMoveAtMs = 0;
        lastRealMs = 0; // 基准置零:下一帧 tick 重建
        beanController = beanFactory.create(gameState, buildBoard(gameState.map), beanEvents); // 豆子来自存档,不再 spawnInitial
        if (events != null) {
            events.onPhaseChanged(GamePhase.PAUSED);
            if (restored.debuffRemainingMs() > 0) {
                events.onDebuffChanged(restored.debuffRemainingMs()); // HUD 徽章/横幅随读档恢复
            }
        }
    }

    /**
     * 唯一推进入口(view 每帧调用):
     * 非 RUNNING 直接返回(=暂停/待开始/结束全冻结)→ 累计 gameTimeMs
     * → 到节拍 stepOnce() → debuff 递减 → BeanController.tick → maybeSpeedUp
     */
    public void tick(long realNowMs) {
        if (gameState == null || lastRealMs == 0 || gameState.phase() != GamePhase.RUNNING) {
            lastRealMs = realNowMs; // 冻结期只重建时钟基准,恢复时 Δ 仍为一帧
            return;
        }

        long delta = realNowMs - lastRealMs;
        lastRealMs = realNowMs;
        gameState.advanceGameTime(delta);

        if (gameState.gameTimeMs() >= nextMoveAtMs) {
            stepOnce();
            nextMoveAtMs += gameState.intervalMs();
            if (gameState.phase() != GamePhase.RUNNING) {
                return; // 本帧内终局:debuff/豆子/提速不再推进
            }
        }

        if (gameState.debuffRemainingMs() > 0) {
            long remaining = Math.max(0L, gameState.debuffRemainingMs() - delta);
            gameState.setDebuffRemainingMs(remaining);
            if (remaining == 0 && events != null) {
                events.onDebuffChanged(0);
            }
        }

        beanController.tick(gameState.gameTimeMs());
        maybeSpeedUp();
    }

    /** 转发 InputController(0.15 s 窗口判定在接收时完成) */
    public void queueInput(Direction dir, long realMs) {
        inputController.queueInput(dir, realMs);
        if (gameState.phase() == GamePhase.READY) {
            gameState.setPhase(GamePhase.RUNNING); // 任意方向键开局
            if (events != null) {
                events.onPhaseChanged(GamePhase.RUNNING);
            }
        }
    }

    /** RUNNING → PAUSED(游戏时间不累计 → 豆时限/倒计时/节拍全冻结);事件通知 */
    public void pause() {
        if (gameState.phase() != GamePhase.RUNNING) {
            return;
        }
        gameState.setPhase(GamePhase.PAUSED);
        if (events != null) {
            events.onPhaseChanged(GamePhase.PAUSED);
        }
    }

    /** PAUSED → RUNNING;事件通知 */
    public void resume() {
        if (gameState.phase() != GamePhase.PAUSED) {
            return;
        }
        gameState.setPhase(GamePhase.RUNNING);
        if (events != null) {
            events.onPhaseChanged(GamePhase.RUNNING);
        }
    }

    /** 结算画面按 R:以同难度/地图重开(newGame(当前 options)) */
    public void restart() {
        newGame(options);
    }

    /** phase=FINISHED、清 debuff、触发 onGameOver;由 stepOnce 内部或外部调用 */
    public void finish(GameOverReason reason) {
        gameState.finish(reason);
        if (gameState.debuffRemainingMs() > 0) {
            gameState.setDebuffRemainingMs(0);
            if (events != null) {
                events.onDebuffChanged(0);
            }
        }
        if (events != null) {
            events.onPhaseChanged(GamePhase.FINISHED);
            events.onGameOver(reason);
        }
    }

    /** view 渲染只读句柄(类型层面只读) */
    public ReadOnlyGameState state() {
        return gameState;
    }

    /** view 订阅事件 */
    public void registerEvents(GameEvents events) {
        this.events = events;
    }

    /** 保存转发:装配方(view)在「保存游戏」时调用,由本类把真实 GameState 交 SaveController 落盘(BR-06);未开局忽略 */
    public void saveTo(SaveController saveController) {
        if (saveController != null && gameState != null) {
            saveController.saveNow(gameState);
        }
    }

    /** 私有流程 stepOnce():resolveTurn → wrap → 障碍/自身/豆子判定 → 效果结算/普通走格 */
    private void stepOnce() {
        // 当前头方向:由头与第二节几何反推(单节蛇兜底向右)
        List<Point> body = gameState.snake().body();
        Direction headDir = Direction.RIGHT;
        if (body.size() >= 2) {
            Point h0 = body.get(0);
            Point h1 = body.get(1);
            for (Direction d : Direction.values()) {
                if (d.dy() == h0.row - h1.row && d.dx() == h0.col - h1.col) {
                    headDir = d;
                    break;
                }
            }
        }

        // 转向判定(0.15 s 窗口已在接收时完成;debuff 期间整体取反)
        Direction dir = inputController.resolveTurn(headDir, gameState.debuffRemainingMs() > 0);

        // 下一格 + 穿墙环化(与 Board.wrap 同语义;Board 注入地图后替换为一行)
        Point head = gameState.snake().head();
        Point next = new Point(head.row + dir.dy(), head.col + dir.dx());
        int row = ((next.row % BoardConfig.ROWS) + BoardConfig.ROWS) % BoardConfig.ROWS;
        int col = ((next.col % BoardConfig.COLS) + BoardConfig.COLS) % BoardConfig.COLS;
        next = new Point(row, col);

        // 障碍判定(含穿墙穿出位置为障碍)
        if (gameState.map().obstacleAt(next)) {
            finish(GameOverReason.OBSTACLE_HIT);
            return;
        }
        // 撞自身
        if (gameState.snake().contains(next)) {
            finish(GameOverReason.SELF_COLLISION);
            return;
        }

        // 豆子命中
        Bean hit = null;
        for (Bean bean : gameState.beans()) {
            if (next.equals(bean.pos)) {
                hit = bean;
                break;
            }
        }

        if (hit == null) {
            gameState.snake().step(next, false); // 普通走格:头进尾去
            return;
        }

        // 效果结算:分值 → 蛇身 → 大毒 debuff(B 定稿:数值一律读 BeanConfig 表,不再自行掷随机)
        gameState.addScore(BeanConfig.scoreOf(hit.type));
        int tailEffect = BeanConfig.tailEffectOf(hit.type); // +1 长一节 / 金豆 -2(固定消除 2 节) / 毒类 0=不变
        gameState.snake().step(next, tailEffect > 0);
        if (tailEffect < 0) {
            int shrink = Math.min(-tailEffect, gameState.snake().length() - 1); // 下限 1 节由调用方保证
            if (shrink > 0) {
                gameState.snake().shrink(shrink);
            }
        }
        if (hit.type == BeanType.BIG_POISON) {
            gameState.setDebuffRemainingMs(BeanConfig.DEBUFF_MS); // 再吃重置 2 秒
            if (events != null) {
                events.onDebuffChanged(BeanConfig.DEBUFF_MS);
            }
        }

        if (events != null) {
            events.onBeanEaten(hit.type, next);
        }
        beanController.onBeanEaten(hit, gameState.gameTimeMs());

        // 通关判定:蛇身占满全部可通行格(无空格)→ CLEARED
        if (gameState.snake().length() >= passableCellCount()) {
            finish(GameOverReason.CLEARED);
            return;
        }

        if (gameState.score() < 0) {
            finish(GameOverReason.SCORE_NEGATIVE); // 扣分后总分为负 → 判负
        }
    }

    /** 可通行格数(= ROWS×COLS - 障碍数);Board 注入地图后替换为 countFree(蛇身) == 0 判定 */
    private int passableCellCount() {
        int count = 0;
        for (int r = 0; r < BoardConfig.ROWS; r++) {
            for (int c = 0; c < BoardConfig.COLS; c++) {
                if (!gameState.map().obstacleAt(new Point(r, c))) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 按本局地图建棋盘(每局新建):逐格收集障碍集(判定同 passableCellCount 口径);map 为 null 时按空棋盘建 */
    private Board buildBoard(GameMap map) {
        Set<Point> obstacles = new HashSet<>();
        if (map != null) {
            for (int r = 0; r < BoardConfig.ROWS; r++) {
                for (int c = 0; c < BoardConfig.COLS; c++) {
                    Point p = new Point(r, c);
                    if (map.obstacleAt(p)) {
                        obstacles.add(p);
                    }
                }
            }
        }
        return new Board(BoardConfig.ROWS, BoardConfig.COLS, obstacles);
    }

    /** 私有流程 maybeSpeedUp():score 达下一档阈值 → intervalMs 下调,仅升档不回退 */
    private void maybeSpeedUp() {
        long target = SpeedConfig.tierFor(gameState.score());
        long bounded = Math.max(target, SpeedConfig.MIN_INTERVAL_MS); // 钳最快档下限
        if (bounded < gameState.intervalMs()) { // 仅升档不回退(吃毒扣分不提速)
            gameState.setIntervalMs(bounded);
        }
    }
}
