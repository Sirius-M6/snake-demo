package controller;

import java.util.List;

import config.BeanConfig;
import config.BoardConfig;
import config.SpeedConfig;
import model.Bean;
import model.BeanType;
import model.Direction;
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

    /** 豆子调度(B 模块,装配时注入) */
    private final BeanController beanController;

    /** 输入判定(C 模块,装配时注入) */
    private final InputController inputController;

    /** 事件回调(view 经 registerEvents 订阅;未订阅时为 null,触发点判空) */
    private GameEvents events;

    /** 装配:MainApp 注入豆子调度与输入判定 */
    public GameController(BeanController beanController, InputController inputController) {
        this.beanController = beanController;
        this.inputController = inputController;
    }

    /** 建局:按难度取 intervalMs;蛇置中;BeanController.spawnInitial();phase=READY;通知事件 */
    public void newGame(GameOptions options) {
        this.options = options;

        GameState state = new GameState();
        state.difficulty = options.getDifficulty();
        state.map = options.getMap();
        state.snake = new Snake(); // 蛇置中:待 Snake 初始坐标入口定稿后接入 BoardConfig.snakeInit()
        state.setIntervalMs(SpeedConfig.initIntervalMs(options.getDifficulty()));
        state.setPhase(GamePhase.READY);
        gameState = state;

        nextMoveAtMs = 0;
        lastRealMs = 0; // 时钟基准置零:下一帧 tick 重建基准,避免跨局 Δ 误计
        beanController.spawnInitial();

        if (events != null) {
            events.onPhaseChanged(GamePhase.READY);
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

        // 效果结算:分值 → 蛇身 → 大毒 debuff
        gameState.addScore(BeanConfig.scoreOf(hit.type));
        if (hit.type == BeanType.SMALL || hit.type == BeanType.BIG) {
            gameState.snake().step(next, true); // 长 1 节
        } else if (hit.type == BeanType.GOLD) {
            gameState.snake().step(next, false);
            int shrink = 2 + (int) (Math.random() * 2); // 消除 2~3 节(随机量联调归 BeanController 掷)
            if (shrink > gameState.snake().length() - 1) {
                shrink = gameState.snake().length() - 1; // 下限 1 节由调用方保证
            }
            if (shrink > 0) {
                gameState.snake().shrink(shrink);
            }
        } else if (hit.type == BeanType.BIG_POISON) {
            gameState.snake().step(next, false);
            gameState.setDebuffRemainingMs(BeanConfig.DEBUFF_MS); // 再吃重置 2 秒
            if (events != null) {
                events.onDebuffChanged(BeanConfig.DEBUFF_MS);
            }
        } else {
            gameState.snake().step(next, false); // POISON:只扣分不减身
        }

        if (events != null) {
            events.onBeanEaten(hit.type, next);
        }
        beanController.onBeanEaten(hit, gameState.gameTimeMs());

        if (gameState.score() < 0) {
            finish(GameOverReason.SCORE_NEGATIVE); // 扣分后总分为负 → 判负
        }
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
