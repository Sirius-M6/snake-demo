package view;

import controller.GameController;
import controller.SaveController;
import model.GameOptions;

/**
 * 页面切换管理(单 Stage 场景路由);持有主界面/难度与地图选择页/记录页视图
 * (游戏场景视图待 showGame 填充时再持有)
 */
public class PageRouter {

    /** 主界面 */
    private final MainMenuView mainMenuView;

    /** 难度/地图选择页 */
    private final GameSetupView gameSetupView;

    /** 记录页 */
    private final RecordsView recordsView;

    /** 构造:注入控制器与选择保持,装配主界面、选择页与记录页视图 */
    public PageRouter(GameController gameController, SaveController saveController, GameOptions gameOptions) {
        this.mainMenuView = new MainMenuView(gameController, saveController, gameOptions, this);
        this.gameSetupView = new GameSetupView(gameController, gameOptions, this);
        this.recordsView = new RecordsView(saveController, this);
    }

    /** 显示主界面 */
    public void showMenu() {
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

    /** 进入游戏场景(启动 GameView 渲染循环) */
    public void showGame() {
    }
}
