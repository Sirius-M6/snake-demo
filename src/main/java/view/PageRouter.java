package view;

import config.UiConfig;
import controller.GameController;
import controller.SaveController;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.GameOptions;
import model.ReadOnlyGameState;

/**
 * 页面切换管理(单 Stage 场景路由);持有主界面/难度与地图选择页/记录页/游戏场景视图
 * (游戏场景视图由 showGame 懒装配)
 */
public class PageRouter {

    /** 主界面 */
    private final MainMenuView mainMenuView;

    /** 难度/地图选择页 */
    private final GameSetupView gameSetupView;

    /** 记录页 */
    private final RecordsView recordsView;

    /** 游戏主控制器(懒装配 GameView 与事件注册用) */
    private final GameController gameController;

    /** 存档控制器(懒装配时注入 GameView 的「保存游戏」出口,BR-06) */
    private final SaveController saveController;

    /** 游戏场景视图(工作量最大,进入游戏场景时才懒创建) */
    private GameView gameView;

    /** 游戏场景窗口(已显示时置前复用,避免重复开窗) */
    private Stage gameStage;

    /** 构造:注入控制器与选择保持,装配主界面、选择页与记录页视图 */
    public PageRouter(GameController gameController, SaveController saveController, GameOptions gameOptions) {
        this.gameController = gameController;
        this.saveController = saveController;
        this.mainMenuView = new MainMenuView(gameController, saveController, gameOptions, this);
        this.gameSetupView = new GameSetupView(gameController, gameOptions, this);
        this.recordsView = new RecordsView(saveController, this);
    }

    /** 显示主界面:游戏窗口若在显示则关闭(结算画面 Esc 退出→回到单窗口主菜单),再弹出主菜单 */
    public void showMenu() {
        if (gameStage != null && gameStage.isShowing()) {
            gameStage.close();
        }
        mainMenuView.showMainMenu();
    }

    /** 显示难度选择页 */
    public void showDifficulty() {
        gameSetupView.showDifficultyPage();
    }

    /** 显示地图选择页 */
    public void showMap() {
        gameSetupView.showMapPage();
    }

    /** 显示记录页 */
    public void showRecords() {
        recordsView.showRecords();
    }

    /**
     * 显示游戏场景:懒装配 GameView(注入 controller、路由出口与存档出口并注册事件)后启动新 Stage 显示;
     * 已显示时置前复用;显示后启动渲染循环(tick 推进 + 真状态渲染)并按当前相位对齐弹层
     */
    public void showGame() {
        // 游戏场景已显示 → 置前复用,避免重复开窗
        if (gameStage != null && gameStage.isShowing()) {
            gameStage.toFront();
            return;
        }
        if (gameView == null) {
            gameView = new GameView(gameController);
            gameView.attachRouter(this); // Esc 结算后回主界面的导航出口
            gameView.attachSave(saveController); // 暂停弹层「保存游戏」出口(BR-06)
            gameController.registerEvents(gameView); // 事件订阅(特效/弹层联动)
        }
        Scene scene = new Scene(gameView, UiConfig.WINDOW_W, UiConfig.WINDOW_H); // 主舞台尺寸自 UiConfig:宽 = 棋盘宽 500,高 = 表头 25 + 棋盘 500
        // 样式表挂载(HUD/弹层文字规格,同 PreviewApp 挂载点)
        scene.getStylesheets().add(PageRouter.class.getResource("/css/app.css").toExternalForm());
        gameStage = new Stage();
        gameStage.setTitle("贪吃蛇");
        gameStage.setResizable(false);
        gameStage.setScene(scene);
        gameStage.centerOnScreen();
        gameStage.show();
        gameView.startRenderLoop(); // 渲染循环驱动:tick 推进 + render 真状态
        // 进入场景相位对齐:弹层显隐由相位事件驱动,首次进入(如"继续游戏"读档)时相位事件早于事件订阅会丢,此处按当前相位补发一次
        ReadOnlyGameState current = gameController.state();
        if (current != null) {
            gameView.onPhaseChanged(current.phase());
        }
    }
}
