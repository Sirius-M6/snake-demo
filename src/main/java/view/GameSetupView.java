package view;

import config.MapCatalog;
import config.UiConfig;
import controller.GameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
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
import model.Difficulty;
import model.GameMap;
import model.GameOptions;
import view.widgets.PrimaryButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 难度选择页与地图选择页(两个独立弹窗):页面尺寸依照 UiConfig,按键与样式风格与 MainMenuView 保持一致;
 * 难度页三键(低/中/高)、地图页五键(按 map.md 名称),按钮垂直排列,选中按钮变蛇绿高亮;
 * 难度/地图选中保持于 GameOptions;右上角返回键回主界面,开始键调 GameController.newGame(GameOptions)
 */
public class GameSetupView {

    /** 米白色背景(同 MainMenuView) */
    private static final String COLOR_BG_CREAM = "#FDF6DB";

    /** 按钮按键颜色(淡橙,同 MainMenuView) */
    private static final String COLOR_BUTTON = "#FDE6BF";

    /** 选中高亮色(蛇绿,取 MainMenuView 标题渐变绿段) */
    private static final String COLOR_SELECTED = "#85CD33";

    /** 按钮文字色(焦糖棕,同 MainMenuView) */
    private static final String COLOR_TEXT_BUTTON = "#8D6E63";

    /** 选中按钮文字色(白,压蛇绿底) */
    private static final String COLOR_TEXT_SELECTED = "#FFFFFF";

    /** hover 色板:淡粉/淡黄/淡蓝/淡绿/淡紫(同 MainMenuView) */
    private static final String COLOR_HOVER_PINK = "#FFD6E0";
    private static final String COLOR_HOVER_YELLOW = "#FFF9C4";
    private static final String COLOR_HOVER_BLUE = "#D6EAF8";
    private static final String COLOR_HOVER_GREEN = "#D5F5E3";
    private static final String COLOR_HOVER_PURPLE = "#E8DAEF";

    /** 可爱圆润字体:优先彩云,缺省回退微软雅黑(同 MainMenuView) */
    private static final String FONT_ROUND = "-fx-font-family: 'ST Caiyun', '华文彩云', 'Microsoft YaHei', '微软雅黑';";

    /** 标题渐变(左→右):粉 → 黄 → 蛇绿 → 蓝,与 MainMenuView 标题同色系 */
    private static final LinearGradient TITLE_GRADIENT = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#FF8FAB")),
            new Stop(0.33, Color.web("#FFC94D")),
            new Stop(0.66, Color.web(COLOR_SELECTED)),
            new Stop(1.0, Color.web("#5FB7E8")));

    /** 游戏主控制器:开始键建局 */
    private final GameController gameController;

    /** 主界面选择保持(难度 + 地图,选中写入/进入时读回) */
    private final GameOptions gameOptions;

    /** 页面路由:返回主界面/进入游戏场景 */
    private final PageRouter pageRouter;

    /** 当前选中难度(默认低;难度页高亮依据) */
    private Difficulty selectedDifficulty = Difficulty.EASY;

    /** 当前选中地图(默认第一张图;地图页高亮依据) */
    private GameMap selectedMap;

    /** 难度键 → 难度映射(选中判定与样式刷新用) */
    private final Map<Button, Difficulty> difficultyByButton = new HashMap<>();

    /** 地图键 → 地图映射(选中判定与样式刷新用) */
    private final Map<Button, GameMap> mapByButton = new HashMap<>();

    /** 选择键 hover 色表(非选中态悬停变色用) */
    private final Map<Button, String> hoverColorByButton = new HashMap<>();

    /** 构造:注入控制器、选择保持与页面路由 */
    public GameSetupView(GameController gameController, GameOptions gameOptions, PageRouter pageRouter) {
        this.gameController = gameController;
        this.gameOptions = gameOptions;
        this.pageRouter = pageRouter;
    }

    /**
     * 显示难度选择页弹窗:标题 + 低/中/高三键垂直排列 + 底部开始键,右上角返回键;
     * 进入时自 GameOptions 读回选中(空 = 默认低),点击选中写入 GameOptions
     */
    public void showDifficultyPage() {
        difficultyByButton.clear();
        mapByButton.clear();
        hoverColorByButton.clear();

        Difficulty keptDifficulty = gameOptions.getDifficulty();
        selectedDifficulty = keptDifficulty == null ? Difficulty.EASY : keptDifficulty;

        showPage("游戏难度", List.of(
                difficultyButton("低", Difficulty.EASY, COLOR_HOVER_BLUE),
                difficultyButton("中", Difficulty.NORMAL, COLOR_HOVER_YELLOW),
                difficultyButton("高", Difficulty.HARD, COLOR_HOVER_PINK)));
    }

    /**
     * 显示地图选择页弹窗:标题 + 地图五键(map.md 名称)垂直排列 + 底部开始键,右上角返回键;
     * 进入时自 GameOptions 读回选中(空 = 默认第一张图),点击选中写入 GameOptions
     */
    public void showMapPage() {
        difficultyByButton.clear();
        mapByButton.clear();
        hoverColorByButton.clear();

        List<GameMap> maps = availableMaps();
        selectedMap = null;
        GameMap keptMap = gameOptions.getMap();
        if (keptMap != null) {
            for (GameMap map : maps) {
                if (map.id.equals(keptMap.id)) {
                    selectedMap = map;
                    break;
                }
            }
        }
        if (selectedMap == null && !maps.isEmpty()) {
            selectedMap = maps.get(0);
        }

        String[] hoverColors = {COLOR_HOVER_BLUE, COLOR_HOVER_GREEN, COLOR_HOVER_PINK, COLOR_HOVER_YELLOW, COLOR_HOVER_PURPLE};
        List<Button> mapButtons = new ArrayList<>();
        for (int i = 0; i < maps.size(); i++) {
            mapButtons.add(mapButton(maps.get(i), hoverColors[i % hoverColors.length]));
        }
        showPage("地图", mapButtons);
    }

    /**
     * 选择页通用骨架:渐变标题 + 垂直排列的选择键 + 底部开始键,右上角返回键;
     * 米白背景点缀小圆点;开始键按当前选择建局并进入游戏场景,返回键回主界面(选中保持于 GameOptions)
     */
    private void showPage(String titleText, List<Button> choiceButtons) {
        Text title = new Text(titleText);
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; " + FONT_ROUND);
        title.setFill(TITLE_GRADIENT);

        VBox choiceBox = new VBox(12);
        choiceBox.setAlignment(Pos.CENTER);
        choiceBox.getChildren().addAll(choiceButtons);

        Button startButton = createMenuButton("开始", COLOR_HOVER_GREEN);
        startButton.setPrefSize(240, 44);
        startButton.setOnAction(e -> {
            gameController.newGame(gameOptions);
            pageRouter.showGame();
            closeWindow(startButton);
        });

        VBox contentBox = new VBox(14, title, choiceBox, startButton);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(40, 24, 32, 24));
        VBox.setMargin(title, new Insets(0, 0, 6, 0));
        VBox.setMargin(startButton, new Insets(20, 0, 0, 0));

        // 返回键:右上角,回主界面(选中已写入 GameOptions,保持)
        Button backButton = createMenuButton("返回", COLOR_HOVER_BLUE);
        backButton.setPrefSize(72, 36);
        backButton.setOnAction(e -> {
            pageRouter.showMenu();
            closeWindow(backButton);
        });

        StackPane root = new StackPane();
        root.setPrefSize(UiConfig.WINDOW_W, UiConfig.WINDOW_H);
        root.setStyle("-fx-background-color: " + COLOR_BG_CREAM + ";");
        StackPane.setAlignment(backButton, Pos.TOP_RIGHT);
        StackPane.setMargin(backButton, new Insets(12, 14, 0, 0));
        root.getChildren().addAll(createDotsLayer(), contentBox, backButton);

        // 按进入时的选中态刷新高亮
        refreshChoiceStyles();

        Stage pageStage = new Stage();
        pageStage.setTitle("贪吃蛇");
        pageStage.initModality(Modality.APPLICATION_MODAL);
        pageStage.setResizable(false);
        pageStage.setScene(new Scene(root));
        pageStage.centerOnScreen();
        pageStage.show();
    }

    /** 地图列表:按需取自 MapCatalog(map.md 五图:初始/管道/雪山/沙滩/田地) */
    private List<GameMap> availableMaps() {
        return MapCatalog.defaultMaps();
    }

    /** 创建难度选择键(文字 + 难度 + hover 色):点击 → 选中并写入 GameOptions */
    private Button difficultyButton(String text, Difficulty difficulty, String hoverColor) {
        Button button = createChoiceButton(text, hoverColor);
        difficultyByButton.put(button, difficulty);
        button.setOnAction(e -> {
            selectedDifficulty = difficulty;
            gameOptions.setDifficulty(difficulty);
            refreshChoiceStyles();
        });
        return button;
    }

    /** 创建地图选择键(地图 + hover 色):点击 → 选中并写入 GameOptions */
    private Button mapButton(GameMap map, String hoverColor) {
        Button button = createChoiceButton(map.displayName, hoverColor);
        mapByButton.put(button, map);
        button.setOnAction(e -> {
            selectedMap = map;
            gameOptions.setMap(map);
            refreshChoiceStyles();
        });
        return button;
    }

    /** 创建选择键:淡橙底加粗;悬停(非选中)→ 变 hoverColor,移出 → 按选中态刷新 */
    private Button createChoiceButton(String text, String hoverColor) {
        Button button = new Button(text);
        button.setPrefSize(240, 44);
        button.setCursor(Cursor.HAND);
        hoverColorByButton.put(button, hoverColor);
        button.setOnMouseEntered(e -> {
            if (!isSelected(button)) {
                button.setStyle(buttonStyle(hoverColor));
            }
        });
        button.setOnMouseExited(e -> refreshChoiceStyles());
        return button;
    }

    /** 创建菜单键(返回/开始):淡橙底加粗;悬停 → 手型 + 背景变 hoverColor,移出恢复 */
    private Button createMenuButton(String text, String hoverColor) {
        Button button = new Button(text);
        button.setCursor(Cursor.HAND);
        button.setStyle(buttonStyle(COLOR_BUTTON));
        button.setOnMouseEntered(e -> button.setStyle(buttonStyle(hoverColor)));
        button.setOnMouseExited(e -> button.setStyle(buttonStyle(COLOR_BUTTON)));
        return button;
    }

    /** 按选中态刷新全部选择键样式:选中 → 蛇绿底白字;未选中 → 淡橙底 */
    private void refreshChoiceStyles() {
        for (Button button : difficultyByButton.keySet()) {
            button.setStyle(isSelected(button) ? selectedStyle() : buttonStyle(COLOR_BUTTON));
        }
        for (Button button : mapByButton.keySet()) {
            button.setStyle(isSelected(button) ? selectedStyle() : buttonStyle(COLOR_BUTTON));
        }
    }

    /** 该键是否处于选中态(难度键按难度判,地图键按地图判) */
    private boolean isSelected(Button button) {
        Difficulty difficulty = difficultyByButton.get(button);
        if (difficulty != null) {
            return difficulty == selectedDifficulty;
        }
        GameMap map = mapByButton.get(button);
        return map != null && map == selectedMap;
    }

    /** 按背景色生成按钮样式串:背景 + 加粗字号(统一委托 PrimaryButton,同 MainMenuView) */
    private String buttonStyle(String backgroundColor) {
        return PrimaryButton.styleWith(backgroundColor);
    }

    /** 选中态样式:蛇绿底 + 白色加粗文字(统一委托 PrimaryButton,后置 text-fill 覆盖基础焦糖棕) */
    private String selectedStyle() {
        return PrimaryButton.styleWith(COLOR_SELECTED, COLOR_TEXT_SELECTED);
    }

    /** 背景点缀层:淡粉/淡黄/淡蓝/淡绿小圆点散落四周(鼠标穿透,不挡按键,同 MainMenuView) */
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

    /** 关闭当前选择页窗口(返回主界面/开始游戏共用) */
    private void closeWindow(Button button) {
        ((Stage) button.getScene().getWindow()).close();
    }
}
