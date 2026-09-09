package view;

import config.BoardConfig;
import controller.GameController;
import controller.GameEvents;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import model.Bean;
import model.BeanType;
import model.GameOverReason;
import model.GamePhase;
import model.Point;
import model.ReadOnlyGameState;

import java.util.List;

/**
 * 游戏主场景(工作量最大):三层 = HUD / Canvas 棋盘 / 特效与弹层;
 * 只收事件转发 + 只读渲染,规则零行;实现 GameEvents
 * 当前阶段:壳 + 单 Canvas 绘制管线(棋盘格 → 障碍 → 蛇 → 豆),内部桩数据自足,不依赖真状态
 */
public class GameView extends BorderPane implements GameEvents {

    /** 棋盘画布(20×20 × 25px = 500×500) */
    private final Canvas boardCanvas;

    /** 游戏控制器引用(壳阶段仅持有;键位转发/节拍循环接入后使用) */
    private final GameController controller;

    // ===== 桩数据(不依赖真状态;接入 GameState 后由 render(state) 换数据源,绘制函数不动) =====

    /** 桩蛇身:第 10 行 6 节,头在右侧(头 → 尾) */
    private static final List<Point> STUB_SNAKE = List.of(
            new Point(10, 9), new Point(10, 8), new Point(10, 7),
            new Point(10, 6), new Point(10, 5), new Point(10, 4));

    /** 桩障碍:两段竖墙(不占边界、不压桩蛇桩豆) */
    private static final List<Point> STUB_OBSTACLES = List.of(
            new Point(3, 5), new Point(4, 5), new Point(5, 5), new Point(6, 5),
            new Point(14, 14), new Point(15, 14), new Point(16, 14));

    /** 桩豆:五类各一颗,分布不重叠 */
    private static final List<Bean> STUB_BEANS = List.of(
            new Bean(BeanType.SMALL, new Point(4, 12), 0L),
            new Bean(BeanType.BIG, new Point(13, 7), 0L),
            new Bean(BeanType.GOLD, new Point(16, 3), 0L),
            new Bean(BeanType.POISON, new Point(5, 16), 0L),
            new Bean(BeanType.BIG_POISON, new Point(15, 10), 0L));

    /** 构造:游戏界面;top 预留 HUD(后续任务),center = 棋盘画布层(特效/弹层后续叠于其上) */
    public GameView(GameController controller) {
        this.controller = controller;
        double size = BoardConfig.ROWS * BoardConfig.CELL_SIZE_PX;
        boardCanvas = new Canvas(size, size);
        // Canvas 不可被布局拉伸,StackPane 负责居中;背景/边框色后续统一走 CSS
        StackPane boardLayer = new StackPane(boardCanvas);
        setCenter(boardLayer);
    }

    /** 启动渲染循环:AnimationTimer 每帧 → GameController.tick(now) → render(state 快照);接入 controller 后填充 */
    public void startRenderLoop() {
        // TODO 渲染循环:AnimationTimer 每帧 tick + render;现为壳,桩预览由外部手动调 render
    }

    /**
     * 单 Canvas 重绘:棋盘格/障碍/蛇/豆(主题配色取自 Palette),不维护 400+ 网格节点;
     * 桩阶段:固定管道主题 + 内部桩数据,state 参数暂不使用(接入真状态后按 state 取地图/蛇/豆)
     */
    public void render(ReadOnlyGameState state) {
        GraphicsContext g = boardCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, boardCanvas.getWidth(), boardCanvas.getHeight());
        // TODO 真状态:Palette p = Palette.ofMapId(state.map().id);数据源换 state 对应只读句柄
        Palette p = Palette.of(Palette.Theme.PIPE);
        drawCheckerboard(g, p);
        drawObstacles(g, p, STUB_OBSTACLES);
        drawSnake(g, p, STUB_SNAKE);
        drawBeans(g, p, STUB_BEANS);
    }

    // ===== 绘制管线(四层按序;函数只吃纯数据,换数据源不换绘制代码) =====

    /** 层 1:棋盘格纹(浅/深交替) */
    private void drawCheckerboard(GraphicsContext g, Palette p) {
        double cell = BoardConfig.CELL_SIZE_PX;
        for (int r = 0; r < BoardConfig.ROWS; r++) {
            for (int c = 0; c < BoardConfig.COLS; c++) {
                g.setFill(((r + c) & 1) == 0 ? p.boardLight() : p.boardDark());
                g.fillRect(c * cell, r * cell, cell, cell);
            }
        }
    }

    /** 层 2:障碍(整格填满,微圆角) */
    private void drawObstacles(GraphicsContext g, Palette p, List<Point> cells) {
        double cell = BoardConfig.CELL_SIZE_PX;
        g.setFill(p.obstacle());
        for (Point pt : cells) {
            g.fillRoundRect(pt.col * cell, pt.row * cell, cell, cell, 3, 3);
        }
    }

    /** 层 3:蛇(头尾渐变:每节取 head.interpolate(tail, t),t 由节位决定;头亮尾暗) */
    private void drawSnake(GraphicsContext g, Palette p, List<Point> body) {
        double cell = BoardConfig.CELL_SIZE_PX;
        double inset = 1.5;
        int n = body.size();
        for (int i = 0; i < n; i++) {
            double t = n <= 1 ? 0.0 : (double) i / (n - 1);
            g.setFill(p.snakeHead().interpolate(p.snakeTail(), t));
            Point pt = body.get(i);
            g.fillRoundRect(pt.col * cell + inset, pt.row * cell + inset,
                    cell - inset * 2, cell - inset * 2, 6, 6);
        }
    }

    /** 层 4:豆(圆形;半径随类别区分:小/毒偏小,大/金偏大,大毒最大) */
    private void drawBeans(GraphicsContext g, Palette p, List<Bean> beans) {
        double cell = BoardConfig.CELL_SIZE_PX;
        for (Bean b : beans) {
            double r = beanRadius(b.type);
            double cx = (b.pos.col + 0.5) * cell;
            double cy = (b.pos.row + 0.5) * cell;
            g.setFill(p.beanColor(b.type));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
    }

    /** 豆半径(px):SMALL 5 / POISON 5.5 / BIG·GOLD 7 / BIG_POISON 8 */
    private double beanRadius(BeanType type) {
        return switch (type) {
            case SMALL -> 5;
            case BIG, GOLD -> 7;
            case POISON -> 5.5;
            case BIG_POISON -> 8;
        };
    }

    // ===== 事件转发(view 实现,controller 触发;联动特效/弹层为后续任务,现阶段空实现) =====

    /** 吃豆(效果结算后)→ 吃豆爆点/飘分特效 */
    @Override
    public void onBeanEaten(BeanType type, Point pos) {
        // TODO 爆点/飘分特效
    }

    /** 新豆补刷成功 → 新豆出现动画(可选) */
    @Override
    public void onBeanRefilled(Bean bean) {
        // TODO 新豆出现动画
    }

    /** 方向颠倒剩余变化 → HUD 颠倒横幅/倒计时 */
    @Override
    public void onDebuffChanged(long remainingMs) {
        // TODO HUD 颠倒横幅
    }

    /** 终局 → 结算画面(最终分/最高分/原因文案;R 重开 / Esc 回主界面) */
    @Override
    public void onGameOver(GameOverReason reason) {
        // TODO 结算弹层
    }

    /** 状态机迁移 → 暂停层显隐等 UI 联动 */
    @Override
    public void onPhaseChanged(GamePhase phase) {
        // TODO 暂停层显隐
    }
}
