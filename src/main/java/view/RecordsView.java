package view;

import config.MapCatalog;
import config.UiConfig;
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
import model.GameMap;
import view.widgets.PrimaryButton;

import java.util.List;

/**
 * 历史记录页:地图按钮列表套用 GameSetupView 地图页样式(垂直排列,无选中高亮、无 refreshChoiceStyles);
 * 点击各地图按键弹出该图最高分(数据只读自 SaveController.highScoreOf,空态「暂无数据」);
 * 右上角返回键回主界面
 */
public class RecordsView {

    /** 米白色背景(同 MainMenuView) */
    private static final String COLOR_BG_CREAM = "#FDF6DB";

    /** 按钮按键颜色(淡橙,同 MainMenuView) */
    private static final String COLOR_BUTTON = "#FDE6BF";

    /** 按钮文字色(焦糖棕,同 MainMenuView) */
    private static final String COLOR_TEXT_BUTTON = "#8D6E63";

    /** 地图键 hover 色板:淡蓝/淡绿/淡粉/淡黄/淡紫(同 GameSetupView 地图页) */
    private static final String COLOR_HOVER_BLUE = "#D6EAF8";
    private static final String COLOR_HOVER_GREEN = "#D5F5E3";
    private static final String COLOR_HOVER_PINK = "#FFD6E0";
    private static final String COLOR_HOVER_YELLOW = "#FFF9C4";
    private static final String COLOR_HOVER_PURPLE = "#E8DAEF";

    /** 可爱圆润字体:优先彩云,缺省回退微软雅黑(同 MainMenuView) */
    private static final String FONT_ROUND = "-fx-font-family: 'ST Caiyun', '华文彩云', 'Microsoft YaHei', '微软雅黑';";

    /** 标题渐变(左→右):粉 → 黄 → 蛇绿 → 蓝,与 MainMenuView 标题同色系 */
    private static final LinearGradient TITLE_GRADIENT = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#FF8FAB")),
            new Stop(0.33, Color.web("#FFC94D")),
            new Stop(0.66, Color.web("#85CD33")),
            new Stop(1.0, Color.web("#5FB7E8")));

    /** 存档控制器:最高分只读查询 */
    private final SaveController saveController;

    /** 页面路由:返回主界面 */
    private final PageRouter pageRouter;

    /** 构造:注入存档控制器与页面路由 */
    public RecordsView(SaveController saveController, PageRouter pageRouter) {
        this.saveController = saveController;
        this.pageRouter = pageRouter;
    }

    /**
     * 显示记录页弹窗:标题 + 地图五键(map.md 名称)垂直排列,右上角返回键;
     * 点击地图键弹出该图最高分(数据只读自 SaveController.highScoreOf,空态「暂无数据」)
     */
    public void showRecords() {
        Text title = new Text("记录");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; " + FONT_ROUND);
        title.setFill(TITLE_GRADIENT);

        VBox mapBox = new VBox(12);
        mapBox.setAlignment(Pos.CENTER);
        String[] hoverColors = {COLOR_HOVER_BLUE, COLOR_HOVER_GREEN, COLOR_HOVER_PINK, COLOR_HOVER_YELLOW, COLOR_HOVER_PURPLE};
        List<GameMap> maps = MapCatalog.defaultMaps();
        for (int i = 0; i < maps.size(); i++) {
            mapBox.getChildren().add(mapButton(maps.get(i), hoverColors[i % hoverColors.length]));
        }

        VBox contentBox = new VBox(14, title, mapBox);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(40, 24, 32, 24));
        VBox.setMargin(title, new Insets(0, 0, 6, 0));

        // 返回键:右上角,回主界面
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

        Stage recordsStage = new Stage();
        recordsStage.setTitle("贪吃蛇");
        recordsStage.initModality(Modality.APPLICATION_MODAL);
        recordsStage.setResizable(false);
        recordsStage.setScene(new Scene(root));
        recordsStage.centerOnScreen();
        recordsStage.show();
    }

    /** 创建地图键(套用 GameSetupView 地图列表样式,无选中高亮):淡橙底加粗,悬停变对应淡色;点击弹该图最高分 */
    private Button mapButton(GameMap map, String hoverColor) {
        Button button = new Button(map.displayName);
        button.setPrefSize(240, 44);
        button.setCursor(Cursor.HAND);
        button.setStyle(buttonStyle(COLOR_BUTTON));
        button.setOnMouseEntered(e -> button.setStyle(buttonStyle(hoverColor)));
        button.setOnMouseExited(e -> button.setStyle(buttonStyle(COLOR_BUTTON)));
        button.setOnAction(e -> showHighScore(map));
        return button;
    }

    /** 弹窗显示该图最高分:数据只读自 SaveController.highScoreOf;无记录(≤0)→ 「暂无数据」 */
    private void showHighScore(GameMap map) {
        int score = saveController.highScoreOf(map.id);
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("最高分");
        alert.setHeaderText(null);
        alert.setContentText(score <= 0 ? map.displayName + ":暂无数据" : map.displayName + "最高分:" + score);
        alert.getDialogPane().setStyle(FONT_ROUND);
        alert.showAndWait();
    }

    /** 创建菜单键(返回):淡橙底加粗;悬停 → 手型 + 背景变 hoverColor,移出恢复 */
    private Button createMenuButton(String text, String hoverColor) {
        Button button = new Button(text);
        button.setCursor(Cursor.HAND);
        button.setStyle(buttonStyle(COLOR_BUTTON));
        button.setOnMouseEntered(e -> button.setStyle(buttonStyle(hoverColor)));
        button.setOnMouseExited(e -> button.setStyle(buttonStyle(COLOR_BUTTON)));
        return button;
    }

    /** 按背景色生成按钮样式串:背景 + 加粗字号(统一委托 PrimaryButton,同 MainMenuView) */
    private String buttonStyle(String backgroundColor) {
        return PrimaryButton.styleWith(backgroundColor);
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

    /** 关闭当前记录页窗口(返回主界面共用) */
    private void closeWindow(Button button) {
        ((Stage) button.getScene().getWindow()).close();
    }
}
