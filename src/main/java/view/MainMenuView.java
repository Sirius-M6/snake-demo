package view;

import config.UiConfig;
import controller.GameController;
import controller.SaveController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.GameOptions;
import view.widgets.PrimaryButton;

/**
 * 主界面:标题 + 五按钮(开始新游戏 / 继续游戏 / 游戏难度 / 地图 / 记录);
 * 继续游戏逻辑:无存档 → 弹"无保存记录"提示(hasSave 判定)
 */
public class MainMenuView {

    /** 米白色背景 */
    private static final String COLOR_BG_CREAM = "#FDF6DB";
    /** 按钮按键颜色(淡橙) */
    private static final String COLOR_BUTTON = "#FDE6BF";
    /** 贪吃蛇标题渐变绿色段(蛇绿) */
    private static final String COLOR_TITLE = "#85CD33";

    /** 开始新游戏按钮 hover 色:淡粉 */
    private static final String COLOR_HOVER_PINK = "#FFD6E0";

    /** 继续游戏按钮 hover 色:淡黄 */
    private static final String COLOR_HOVER_YELLOW = "#FFF9C4";

    /** 游戏难度按钮 hover 色:淡蓝 */
    private static final String COLOR_HOVER_BLUE = "#D6EAF8";

    /** 地图按钮 hover 色:淡绿 */
    private static final String COLOR_HOVER_GREEN = "#D5F5E3";

    /** 记录按钮 hover 色:淡紫 */
    private static final String COLOR_HOVER_PURPLE = "#E8DAEF";

    /** 标题渐变(左→右):粉 → 黄 → 蛇绿 → 蓝,与点缀圆点同色系 */
    private static final LinearGradient TITLE_GRADIENT = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#FF8FAB")),
            new Stop(0.33, Color.web("#FFC94D")),
            new Stop(0.66, Color.web(COLOR_TITLE)),
            new Stop(1.0, Color.web("#5FB7E8")));

    /** 按钮文字色(焦糖棕):清新可爱,替代沉闷的默认黑 */
    private static final String COLOR_TEXT_BUTTON = "#8D6E63";

    /** 可爱圆润字体:优先幼圆,缺省回退微软雅黑 */
    private static final String FONT_ROUND = "-fx-font-family: 'ST Caiyun', '华文彩云', 'Microsoft YaHei', '微软雅黑';";

    /** 游戏主控制器:开始新游戏 / 继续游戏 */
    private final GameController gameController;

    /** 存档控制器:继续游戏 hasSave 判定与读档 */
    private final SaveController saveController;

    /** 主界面选择保持(难度 + 地图,开局传入) */
    private final GameOptions gameOptions;

    /** 页面路由:难度/地图/记录/游戏场景切换 */
    private final PageRouter pageRouter;

    /** 主界面弹窗(已显示时置前复用,避免重复开窗) */
    private Stage menuStage;

    /** 构造:主界面;注入控制器、选择保持与页面路由 */
    public MainMenuView(GameController gameController, SaveController saveController,
                        GameOptions gameOptions, PageRouter pageRouter) {
        this.gameController = gameController;
        this.saveController = saveController;
        this.gameOptions = gameOptions;
        this.pageRouter = pageRouter;
    }

    /**
     * 显示主界面弹窗:贪吃蛇标题(渐变) + 五按钮自上而下(开始新游戏/继续游戏/游戏难度/地图/记录);
     * 米白色背景点缀淡粉/淡黄/淡蓝/淡绿小圆点;鼠标悬停按钮 → 手型,按钮由淡橙分别变淡粉/淡黄/淡蓝/淡绿/淡紫
     */
    public void showMainMenu() {
        // 主界面已显示 → 置前复用,避免多次进入叠加窗口
        if (menuStage != null && menuStage.isShowing()) {
            menuStage.toFront();
            return;
        }

        Text title = new Text("贪吃蛇");
        title.setStyle("-fx-font-size: 44px; -fx-font-weight: bold; " + FONT_ROUND);
        title.setFill(TITLE_GRADIENT);

        Button startButton = createMenuButton("开始新游戏", COLOR_HOVER_PINK);
        Button continueButton = createMenuButton("继续游戏", COLOR_HOVER_YELLOW);
        Button difficultyButton = createMenuButton("游戏难度", COLOR_HOVER_BLUE);
        Button mapButton = createMenuButton("地图", COLOR_HOVER_GREEN);
        Button recordsButton = createMenuButton("记录", COLOR_HOVER_PURPLE);

        startButton.setOnAction(e -> startNewGame());
        continueButton.setOnAction(e -> continueGame());
        difficultyButton.setOnAction(e -> pageRouter.showDifficulty());
        mapButton.setOnAction(e -> pageRouter.showMap());
        recordsButton.setOnAction(e -> pageRouter.showRecords());

        VBox menuBox = new VBox(20, title, startButton, continueButton, difficultyButton, mapButton, recordsButton);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setPadding(new Insets(40, 64, 40, 64));
        VBox.setMargin(title, new Insets(0, 0, 10, 0));

        StackPane root = new StackPane();
        root.setPrefSize(UiConfig.WINDOW_W, UiConfig.WINDOW_H);
        root.setStyle("-fx-background-color: " + COLOR_BG_CREAM + ";");
        root.getChildren().addAll(createDotsLayer(), menuBox);

        menuStage = new Stage();
        menuStage.setTitle("贪吃蛇");
        menuStage.initModality(Modality.APPLICATION_MODAL);
        menuStage.setResizable(false);
        menuStage.setScene(new Scene(root));
        menuStage.centerOnScreen();
        menuStage.show();
    }

    /** 背景点缀层:淡粉/淡黄/淡蓝/淡绿小圆点散落四周(鼠标穿透,不挡按钮) */
    private Pane createDotsLayer() {
        Pane layer = new Pane();
        layer.setMouseTransparent(true);
        layer.getChildren().addAll(
                dot(35, 47, 7, COLOR_HOVER_PINK),
                dot(465, 72, 8, COLOR_HOVER_YELLOW),
                dot(27, 196, 6, COLOR_HOVER_BLUE),
                dot(473, 233, 9, COLOR_HOVER_GREEN),
                dot(49, 362, 7, COLOR_HOVER_GREEN),
                dot(451, 392, 6, COLOR_HOVER_PINK),
                dot(76, 488, 7, COLOR_HOVER_YELLOW),
                dot(421, 495, 8, COLOR_HOVER_BLUE),
                dot(255, 30, 5, COLOR_HOVER_BLUE),
                dot(204, 504, 5, COLOR_HOVER_GREEN));
        return layer;
    }

    /** 生成一枚装饰圆点(位置 + 半径 + 颜色) */
    private Circle dot(double x, double y, double radius, String color) {
        Circle circle = new Circle(x, y, radius);
        circle.setFill(Color.web(color));
        circle.setOpacity(0.9);
        return circle;
    }

    /** 开始新游戏:按当前已选难度/地图开局,进入游戏场景 */
    private void startNewGame() {
        gameController.newGame(gameOptions);
        pageRouter.showGame();
    }

    /** 继续游戏:无存档 → 弹"无保存记录"提示;有存档 → 读档恢复并进入游戏场景 */
    private void continueGame() {
        if (!saveController.hasSave()) {
            showNoSaveDialog();
            return;
        }
        saveController.loadAndResume();
        pageRouter.showGame();
    }

    /** "无保存记录"提示弹窗(关闭后停留主界面) */
    private void showNoSaveDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText("无保存记录");
        alert.getDialogPane().setStyle(FONT_ROUND);
        alert.showAndWait();
    }

    /** 按背景色生成按钮样式串:背景 + 加粗字号(统一委托 PrimaryButton,同 18px 加粗圆润字体) */
    private String buttonStyle(String backgroundColor) {
        return PrimaryButton.styleWith(backgroundColor);
    }

    /** 创建菜单按钮:淡橙底加粗;鼠标悬停 → 手型 + 背景变 hoverColor,移出恢复淡橙 */
    private Button createMenuButton(String text, String hoverColor) {
        Button button = new Button(text);
        button.setPrefSize(240, 44);
        button.setCursor(Cursor.HAND);
        button.setStyle(buttonStyle(COLOR_BUTTON));
        button.setOnMouseEntered(e -> button.setStyle(buttonStyle(hoverColor)));
        button.setOnMouseExited(e -> button.setStyle(buttonStyle(COLOR_BUTTON)));
        return button;
    }
}
