package view;

import config.BeanConfig;
import config.BoardConfig;
import config.UiConfig;
import controller.GameController;
import controller.GameEvents;
import controller.SaveController;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import model.Bean;
import model.BeanType;
import model.Direction;
import model.GameMap;
import model.GameOverReason;
import model.GamePhase;
import model.Point;
import model.ReadOnlyGameState;
import view.fx.BeanBurst;
import view.fx.DirectionBanner;
import view.fx.ScreenFlash;
import view.fx.ScorePopup;
import view.widgets.GameOverOverlay;
import view.widgets.PauseOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 游戏主场景(工作量最大):三层 = HUD / Canvas 棋盘 / 特效与弹层;
 * 只收事件转发 + 只读渲染,规则零行;实现 GameEvents
 * 当前阶段:渲染循环驱动(AnimationTimer 每帧 tick + 真状态渲染)+ 单 Canvas 绘制管线(棋盘格 → 障碍 → 蛇 → 豆)+ HUD(得分/颠倒徽章)+ 键位转发(方向/空格/R/Esc)+ 暂停/结算弹层 + fx 特效接线(爆点/飘分/红闪/颠倒横幅)
 */
public class GameView extends BorderPane implements GameEvents {

    /** 棋盘画布(20×20 × 25px = 500×500) */
    private final Canvas boardCanvas;

    /** 游戏控制器引用(键位转发 + 渲染循环 tick 均已接入) */
    private final GameController controller;

    /** 渲染循环(懒建单例;每帧 tick 推进 + render 真状态;startRenderLoop 幂等启动) */
    private AnimationTimer renderLoop;

    /** 页面路由引用(Esc 结算后回主界面的导航出口;装配时 attachRouter 注入,未注入则 Esc 不导航) */
    private PageRouter router;

    /** 存档控制器引用(「保存游戏」按钮出口;装配时 attachSave 注入,未注入则点击无动作) */
    private SaveController saveController;

    /** 暂停弹层(遮罩 + 三选项卡片;BR-04;显隐随相位联动) */
    private final PauseOverlay pauseOverlay = new PauseOverlay();

    /** 结算弹层(原因/最终分/最高分;BR-51;显隐随终局事件) */
    private final GameOverOverlay gameOverOverlay = new GameOverOverlay();

    /** 瞬时特效宿主层(吃豆爆点/飘分动态挂入,播完各自自移除;与棋盘画布同尺寸对齐) */
    private final Pane transientFxLayer = new Pane();

    /** 方向颠倒横幅(棋盘顶部居中;onDebuffChanged 驱动显隐与倒计时) */
    private final DirectionBanner directionBanner = new DirectionBanner();

    /** 整屏红闪(毒豆扣分/死亡瞬间警示;居叠加层最顶) */
    private final ScreenFlash screenFlash = new ScreenFlash();

    /** 得分标签(HUD 左侧;BR-60 吃豆后实时刷新) */
    private final Label scoreLabel = new Label("得分 0");

    /** debuff 颠倒倒计时徽章(HUD 计分右侧;BR-61 仅 debuff 生效期显示) */
    private final Label debuffBadge = new Label();

    /** 暂停按钮(HUD 右上角常驻;双竖杠图标;点击与空格同语义,BR-05) */
    private final Button pauseButton = new Button();

    /** 暂停图标左竖杠(悬停变色用) */
    private final Line pauseBarLeft = new Line(1, 0, 1, 9);

    /** 暂停图标右竖杠(悬停变色用) */
    private final Line pauseBarRight = new Line(6, 0, 6, 9);

    /** 构造:游戏界面;top = HUD(得分左侧/debuff 徽章计分右侧/暂停按钮最右常驻;BR-60/61/05),center = 棋盘层(画布 → 特效 → 弹层 → 红闪);装配 controller 后安装全局键位转发并接线弹层与 HUD 按钮 */
    public GameView(GameController controller) {
        this.controller = controller;
        double size = BoardConfig.ROWS * BoardConfig.CELL_SIZE_PX;
        boardCanvas = new Canvas(size, size);
        // Canvas 不可被布局拉伸,StackPane 负责居中;背景/边框色后续统一走 CSS
        // 叠加层序(底 → 顶):画布 → 颠倒横幅 → 瞬时特效 → 暂停/结算弹层 → 红闪(警示盖弹层)
        StackPane boardLayer = new StackPane(boardCanvas, directionBanner);
        StackPane.setAlignment(directionBanner, Pos.TOP_CENTER);
        StackPane.setMargin(directionBanner, new Insets(6, 0, 0, 0));
        pinToBoard(transientFxLayer); // 瞬时特效宿主:与画布同尺寸对齐,内部坐标即棋盘像素
        boardLayer.getChildren().add(transientFxLayer);
        boardLayer.getChildren().addAll(pauseOverlay, gameOverOverlay); // 弹层叠于棋盘与特效之上(初始隐藏)
        pinToBoard(screenFlash); // 红闪:与画布同尺寸,居最顶
        boardLayer.getChildren().add(screenFlash);
        setCenter(boardLayer);
        setTop(buildHud());
        if (controller != null) {
            installInput(); // 装配正式 controller 才采键位;预览壳阶段(无 controller)跳过
            wireOverlayActions(); // 弹层按钮动作接线(依赖 controller)
        }
    }

    /** 固定叠加层为棋盘画布同尺寸(StackPane 居中下与画布重合,内部坐标系即棋盘像素坐标);特效层不参与鼠标交互 */
    private void pinToBoard(Pane layer) {
        double size = boardCanvas.getWidth();
        layer.setMinSize(size, size);
        layer.setPrefSize(size, size);
        layer.setMaxSize(size, size);
        layer.setMouseTransparent(true);
    }

    // ===== HUD 层(棋盘上侧:得分 + debuff 颠倒倒计时徽章;BR-60/61,暂停按钮另项) =====

    /** 构建 HUD:左 = 实时得分;计分右侧 = 颠倒徽章;最右 = 常驻暂停按钮(弹性占位);配色随主题(Palette),字体/间距走 CSS(app.css) */
    private HBox buildHud() {
        Palette p = Palette.of(Palette.Theme.PIPE); // TODO 真状态:随 render 同一主题取色
        scoreLabel.getStyleClass().add("hud-score");
        scoreLabel.setTextFill(p.snakeTail());
        debuffBadge.getStyleClass().add("hud-debuff");
        debuffBadge.setTextFill(Color.WHITE);
        // 徽章底色联动触发源(大毒豆深紫),圆角胶囊;内边距/字体规格见 CSS
        debuffBadge.setBackground(new Background(new BackgroundFill(
                p.beanColor(BeanType.BIG_POISON), new CornerRadii(10), Insets.EMPTY)));
        debuffBadge.setVisible(false);
        debuffBadge.setManaged(false);
        configurePauseButton(p);
        Region spacer = new Region(); // 弹性占位:把暂停按钮推到表头最右(按钮常驻,不随相位显隐)
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox hud = new HBox(scoreLabel, debuffBadge, spacer, pauseButton);
        hud.getStyleClass().add("hud");
        hud.setAlignment(Pos.CENTER_LEFT);
        // 表头区固定 UiConfig.HUD_H(25 px):BorderPane 顶栏不挤占棋盘,保证 500×500 画布完整露出
        hud.setMinHeight(UiConfig.HUD_H);
        hud.setPrefHeight(UiConfig.HUD_H);
        hud.setMaxHeight(UiConfig.HUD_H);
        return hud;
    }

    /** 配置暂停按钮:双竖杠图标(形如 ⏸),图标色取主题蛇尾色、悬停加深;不抢键盘焦点(键位走全局过滤器);尺寸/手型见 CSS */
    private void configurePauseButton(Palette p) {
        Color base = p.snakeTail();
        Color hover = base.darker();
        for (Line bar : new Line[]{pauseBarLeft, pauseBarRight}) {
            bar.setStroke(base);
            bar.setStrokeWidth(2.2);
            bar.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        pauseButton.getStyleClass().add("hud-pause");
        pauseButton.setGraphic(new Group(pauseBarLeft, pauseBarRight));
        pauseButton.setFocusTraversable(false);
        pauseButton.setOnMouseEntered(e -> {
            pauseBarLeft.setStroke(hover);
            pauseBarRight.setStroke(hover);
        });
        pauseButton.setOnMouseExited(e -> {
            pauseBarLeft.setStroke(base);
            pauseBarRight.setStroke(base);
        });
    }

    /** 刷新得分显示(BR-60:吃豆立即更新;接线:渲染循环从只读状态读取当前分值) */
    public void updateScore(int score) {
        scoreLabel.setText("得分 " + score);
    }

    /** 刷新颠倒徽章:remainingMs > 0 显示实时倒计时;归 0 隐藏(BR-61 仅生效期显示) */
    private void updateDebuffBadge(long remainingMs) {
        if (remainingMs > 0) {
            debuffBadge.setText(String.format(Locale.ROOT, "颠倒 %.1fs", remainingMs / 1000.0));
            debuffBadge.setVisible(true);
            debuffBadge.setManaged(true);
        } else {
            debuffBadge.setVisible(false);
            debuffBadge.setManaged(false);
        }
    }

    // ===== 键位转发(全局采集:方向/空格/R/Esc → controller;只做按键→语义映射,规则零行) =====

    /** 装配注入页面路由(Esc 结算后回主界面的出口;不注入则 Esc 无导航) */
    public void attachRouter(PageRouter router) {
        this.router = router;
    }

    /** 装配注入存档控制器(暂停弹层「保存游戏」出口,BR-06;不注入则点击无动作) */
    public void attachSave(SaveController saveController) {
        this.saveController = saveController;
    }

    /** 安装全局键位采集:scene 挂载/卸载时注册/注销捕获过滤器(不受焦点影响;暂停弹窗打开时同样生效,BR-05) */
    private void installInput() {
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
            }
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
            }
        });
    }

    /**
     * 按键 → 纯数据转发(只做映射,不判规则):方向键(含开局)带真实时刻入缓冲(0.15 s 窗口在接收时判定);
     * 空格按只读相位暂停/继续;R/Esc 仅结算画面生效(BR-08/51);未识别键不拦截
     */
    private void handleKeyPressed(KeyEvent e) {
        long realMs = System.currentTimeMillis(); // 与 controller/tick 的 realMs 口径一致
        ReadOnlyGameState state = controller.state(); // 只读句柄;C 模块实现前为 null
        switch (e.getCode()) {
            case UP -> controller.queueInput(Direction.UP, realMs);
            case DOWN -> controller.queueInput(Direction.DOWN, realMs);
            case LEFT -> controller.queueInput(Direction.LEFT, realMs);
            case RIGHT -> controller.queueInput(Direction.RIGHT, realMs);
            case SPACE -> togglePause(state);
            case R -> {
                if (isFinished(state)) {
                    controller.restart();
                }
            }
            case ESCAPE -> {
                if (isFinished(state) && router != null) {
                    router.showMenu();
                }
            }
            default -> {
                return; // 未识别键:不消费,交还默认处理
            }
        }
        e.consume();
    }

    /** 空格/暂停按钮:暂停切换(BR-03/05;按只读相位路由:RUNNING → pause,PAUSED → resume,其余忽略;state==null 时直接返回不弹夹层,弹夹层条件见 onPhaseChanged 的 C1 联调备忘) */
    private void togglePause(ReadOnlyGameState state) {
        if (state == null) {
            return;
        }
        switch (state.phase()) {
            case RUNNING -> controller.pause();
            case PAUSED -> controller.resume();
            default -> {
            }
        }
    }

    /** R/Esc 前置:是否处于结算画面(状态未装配实现时视为否,不转发) */
    private boolean isFinished(ReadOnlyGameState state) {
        return state != null && state.phase() == GamePhase.FINISHED;
    }

    // ===== 弹层接线(暂停/结算按钮 → controller;显隐见事件转发区) =====

    /** 弹层与 HUD 按钮动作接线:「继续」/× → resume(BR-05);「终止游戏」→ 终局链路(BR-07);「保存游戏」→ saveTo 转发落盘(BR-06);暂停按钮 → 与空格同语义(BR-05) */
    private void wireOverlayActions() {
        pauseOverlay.setOnResume(controller::resume);
        pauseOverlay.setOnTerminate(() -> controller.finish(GameOverReason.ABANDONED));
        pauseOverlay.setOnSave(() -> {
            if (saveController != null) {
                controller.saveTo(saveController); // controller 持真实 GameState;未注入存档出口则忽略
            }
        });
        pauseButton.setOnAction(e -> togglePause(controller.state())); // 暂停按钮:按只读相位路由,同空格
    }

    /** 启动渲染循环:AnimationTimer 每帧 → GameController.tick(now) → render(controller.state());幂等(重复调用只启一次),未装配 controller 时不启动 */
    public void startRenderLoop() {
        if (renderLoop != null || controller == null) {
            return;
        }
        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long nowMs = System.currentTimeMillis(); // 与键位转发 realMs 同口径(handle 的 nanoTime 只作帧标识)
                controller.tick(nowMs);
                render(controller.state());
            }
        };
        renderLoop.start();
        render(controller.state()); // 立即首帧,不等下一脉冲
    }

    /**
     * 单 Canvas 重绘:棋盘格/障碍/蛇/豆(主题配色取自 Palette),不维护 400+ 网格节点;
     * 数据源 = controller 只读快照;state 为 null(未开局/未装配)时只画默认主题空棋盘
     */
    public void render(ReadOnlyGameState state) {
        GraphicsContext g = boardCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, boardCanvas.getWidth(), boardCanvas.getHeight());
        if (state == null) {
            drawCheckerboard(g, Palette.of(Palette.Theme.PIPE)); // 未开局:空棋盘兜底
            return;
        }
        Palette p = paletteFor(state.map());
        drawCheckerboard(g, p);
        drawObstacles(g, p, obstacleCells(state.map()));
        drawSnake(g, p, state.snake() != null ? state.snake().body() : List.of());
        drawBeans(g, p, state.beans() != null ? state.beans() : List.of());
        updateScore(state.score()); // BR-60:得分单一数据源 = 只读状态,每帧随渲染刷新
    }

    /** 地图 → 主题调色板:未注册主题的地图(初始/沙滩/田地)兜底管道主题,待 A 主题色补齐后放开 */
    private Palette paletteFor(GameMap map) {
        if (map != null) {
            for (Palette.Theme theme : Palette.Theme.values()) {
                if (theme.id.equalsIgnoreCase(map.id)) {
                    return Palette.of(theme);
                }
            }
        }
        return Palette.of(Palette.Theme.PIPE);
    }

    /** 地图障碍格集合(全网格扫描 obstacleAt;20×20 每帧开销可忽略) */
    private List<Point> obstacleCells(GameMap map) {
        List<Point> cells = new ArrayList<>();
        if (map != null) {
            for (int r = 0; r < BoardConfig.ROWS; r++) {
                for (int c = 0; c < BoardConfig.COLS; c++) {
                    Point pt = new Point(r, c);
                    if (map.obstacleAt(pt)) {
                        cells.add(pt);
                    }
                }
            }
        }
        return cells;
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

    /**
     * 层 4:豆(五件套:形状+颜色双通道——小豆红点 / 大豆橙大圆 / 金豆金星 / 毒豆紫圆×纹 / 大毒豆暗紫大圆×纹;
     * 叉线色取底色 darker(),不另设常量,随豆色联动)
     */
    private void drawBeans(GraphicsContext g, Palette p, List<Bean> beans) {
        for (Bean b : beans) {
            drawBean(g, p, b);
        }
    }

    /** 单颗豆:按类别分派绘制 */
    private void drawBean(GraphicsContext g, Palette p, Bean b) {
        double cell = BoardConfig.CELL_SIZE_PX;
        double cx = (b.pos.col + 0.5) * cell;
        double cy = (b.pos.row + 0.5) * cell;
        switch (b.type) {
            case SMALL -> fillCircle(g, cx, cy, 5, p.beanColor(BeanType.SMALL));
            case BIG -> fillCircle(g, cx, cy, 7, p.beanColor(BeanType.BIG));
            case GOLD -> fillStar(g, cx, cy, 8, p.beanColor(BeanType.GOLD));
            case POISON -> {
                fillCircle(g, cx, cy, 6, p.beanColor(BeanType.POISON));
                drawCross(g, cx, cy, 6, p.beanColor(BeanType.POISON).darker(), 2.2);
            }
            case BIG_POISON -> {
                fillCircle(g, cx, cy, 8, p.beanColor(BeanType.BIG_POISON));
                drawCross(g, cx, cy, 8, p.beanColor(BeanType.BIG_POISON).darker(), 3);
            }
        }
    }

    /** 实心圆(中心 + 半径) */
    private void fillCircle(GraphicsContext g, double cx, double cy, double r, Color color) {
        g.setFill(color);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    /** ×纹:两条对角斜线,端点落在半径 0.75 处,圆润线帽 */
    private void drawCross(GraphicsContext g, double cx, double cy, double r, Color color, double lineWidth) {
        double len = r * 0.75;
        g.setStroke(color);
        g.setLineWidth(lineWidth);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(cx - len, cy - len, cx + len, cy + len);
        g.strokeLine(cx - len, cy + len, cx + len, cy - len);
    }

    /** 金星:标准五角星(10 顶点内外交替,起于正上方;内半径 = 外半径 × sin18°/sin54° ≈ 0.382) */
    private void fillStar(GraphicsContext g, double cx, double cy, double outerR, Color color) {
        double innerR = outerR * 0.382;
        int points = 10;
        double[] xs = new double[points];
        double[] ys = new double[points];
        for (int i = 0; i < points; i++) {
            double angle = Math.toRadians(-90 + i * 360.0 / points);
            double r = (i % 2 == 0) ? outerR : innerR;
            xs[i] = cx + r * Math.cos(angle);
            ys[i] = cy + r * Math.sin(angle);
        }
        g.setFill(color);
        g.fillPolygon(xs, ys, points);
    }

    // ===== 事件转发(view 实现,controller 触发;事件参数 → 特效/弹层联动,不猜状态) =====

    /** 吃豆(效果结算后)→ 爆点 + 飘分(毒豆用警示红);毒豆追加整屏红闪(BR-23 debuff 另由 onDebuffChanged 提示) */
    @Override
    public void onBeanEaten(BeanType type, Point pos) {
        double cell = BoardConfig.CELL_SIZE_PX;
        double cx = (pos.col + 0.5) * cell; // 格 → 像素中心(换算由调用方做)
        double cy = (pos.row + 0.5) * cell;
        Palette p = Palette.of(Palette.Theme.PIPE); // TODO 真状态:随主题取色
        boolean toxic = type == BeanType.POISON || type == BeanType.BIG_POISON;

        BeanBurst burst = new BeanBurst();
        transientFxLayer.getChildren().add(burst);
        burst.burstAt(cx, cy, p.beanColor(type));

        ScorePopup popup = new ScorePopup();
        transientFxLayer.getChildren().add(popup);
        // 分值文本单一数据源取 BeanConfig.scoreOf(若显示 +0 说明分值表桩未填,不另抄一份)
        popup.popAt(cx, cy, String.format(Locale.ROOT, "%+d", BeanConfig.scoreOf(type)),
                toxic ? ScorePopup.DAMAGE_COLOR : p.beanColor(type));

        if (toxic) {
            screenFlash.flash(); // 毒豆扣分瞬间警示
        }
    }

    /** 新豆补刷成功 → 新豆出现动画(设计标注可选;无专属 fx 组件,暂不播放) */
    @Override
    public void onBeanRefilled(Bean bean) {
    }

    /** 方向颠倒剩余变化 → HUD 颠倒徽章 + 棋盘顶部横幅(实时倒计时/结束隐藏) */
    @Override
    public void onDebuffChanged(long remainingMs) {
        updateDebuffBadge(remainingMs);
        directionBanner.update(remainingMs);
    }

    /** 终局 → 死亡红闪(主动终止/通关不播)+ 结算弹层:原因/最终分即时填充;最高分待 M2 接 SaveController 后替换占位 0(BR-51;R 重开 / Esc 回主界面已由键位转发支持) */
    @Override
    public void onGameOver(GameOverReason reason) {
        pauseOverlay.hideOverlay(); // 终局必收起暂停弹层(与相位通知幂等)
        if (reason != GameOverReason.ABANDONED && reason != GameOverReason.CLEARED) {
            screenFlash.flash(); // 撞击/负分死亡的瞬间警示
        }
        ReadOnlyGameState state = controller != null ? controller.state() : null; // 只读句柄;壳阶段可能为 null
        int finalScore = state != null ? state.score() : 0;
        // TODO M2:最高分 = SaveController.highScoreOf(state.map().id)(装配注入后)
        gameOverOverlay.showResult(reason, finalScore, 0);
    }

    /**
     * 状态机迁移 → 暂停弹层显隐(BR-04:PAUSED 显示;继续/复位/终局随相位收起,BR-05/07)。
     * 夹层显隐的唯一驱动点:view 不做本地显隐判断,只认 controller 发来的相位事件。
     *
     * ── 给 C1 的联调备忘:什么情况下才会弹出夹层 ──
     * 入口(HUD 暂停按钮 / 空格键,均走 togglePause 相位路由):
     *   state() 返回 null        → 直接忽略,永不弹夹层;
     *   phase == RUNNING         → controller.pause();
     *   phase == PAUSED          → controller.resume();
     *   phase == READY/FINISHED  → 忽略。
     * C1 实现 GameController 需同时满足三条,夹层才会弹出/收起:
     *   1. state() 返回真实 ReadOnlyGameState(否则 togglePause 第一行就 return);
     *   2. registerEvents(GameEvents) 真实保存订阅者(PageRouter.showGame 已调用注册);
     *   3. 相位迁移后必须向订阅者发事件:
     *      pause()  RUNNING→PAUSED  → 通知 onPhaseChanged(PAUSED)  → 弹夹层;
     *      resume() PAUSED→RUNNING  → 通知 onPhaseChanged(RUNNING) → 收夹层;
     *      newGame()/restart() phase=READY → 通知 READY → 收夹层(开局不弹);
     *      finish() phase=FINISHED  → 通知 FINISHED → 收夹层(onGameOver 内另有幂等收起)。
     * 夹层内按钮已接线:「继续」/× → resume(BR-05);「终止游戏」→ finish(ABANDONED)(BR-07)。
     */
    @Override
    public void onPhaseChanged(GamePhase phase) {
        if (phase == GamePhase.PAUSED) {
            pauseOverlay.showOverlay();
        } else {
            pauseOverlay.hideOverlay();
        }
        if (phase != GamePhase.FINISHED) {
            gameOverOverlay.hideOverlay(); // 离开终局相位(R 重开/读档装载/新局):收起结算层,避免盖住新局画面
        }
    }
}
