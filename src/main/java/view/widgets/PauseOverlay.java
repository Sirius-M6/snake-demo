package view.widgets;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.BeanType;
import view.Palette;

/**
 * 暂停弹层(游戏界面内叠层):半透明遮罩 + 居中卡片 —— 三选项「继续 / 保存游戏 / 终止游戏」+ 右上角 ×(BR-04/05);
 * 显隐由 GameView 按相位联动(PAUSED 显示);按钮动作经 setOnXxx 注入,未接线时点击无动作;
 * 配色取自 Palette(桩阶段定管道主题),字体/间距/尺寸走 CSS(app.css)
 */
public class PauseOverlay extends StackPane {

    /** 「继续」与 × 的动作(BR-05;装配接 controller.resume) */
    private Runnable onResume;

    /** 「保存游戏」的动作(BR-06;待 M2 装配 SaveController 后接线) */
    private Runnable onSave;

    /** 「终止游戏」的动作(BR-07;装配接 controller.finish(ABANDONED)) */
    private Runnable onTerminate;

    /** 构造:搭遮罩背景 + 居中卡片;初始隐藏(由相位联动显示) */
    public PauseOverlay() {
        setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.45), CornerRadii.EMPTY, Insets.EMPTY)));
        getChildren().add(buildCard());
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setVisible(false);
        setManaged(false);
    }

    /** 接线:「继续」与右上角 ×(BR-05:两者均视为继续并关闭弹窗) */
    public void setOnResume(Runnable onResume) {
        this.onResume = onResume;
    }

    /** 接线:「保存游戏」(BR-06) */
    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    /** 接线:「终止游戏」(BR-07) */
    public void setOnTerminate(Runnable onTerminate) {
        this.onTerminate = onTerminate;
    }

    /** 显示弹层(PAUSED 相位联动;双切 visible/managed,显示时才参与布局) */
    public void showOverlay() {
        setManaged(true);
        setVisible(true);
    }

    /** 隐藏弹层(继续/终局/复位联动) */
    public void hideOverlay() {
        setVisible(false);
        setManaged(false);
    }

    /** 卡片:米白底 + 圆角 + 投影;标题/按钮居中,× 锚卡片右上角 */
    private StackPane buildCard() {
        Palette p = Palette.of(Palette.Theme.PIPE); // TODO 真状态:随主题取色(与渲染/HUD 一致)
        Label title = new Label("游戏暂停");
        title.getStyleClass().add("overlay-title");
        title.setTextFill(p.snakeTail());

        PrimaryButton resumeBtn = makeButton("继续", p.snakeHead(), Color.WHITE);
        resumeBtn.setOnAction(e -> run(onResume));
        PrimaryButton saveBtn = makeButton("保存游戏", p.boardDark(), p.snakeTail());
        saveBtn.setOnAction(e -> run(onSave));
        PrimaryButton terminateBtn = makeButton("终止游戏", p.beanColor(BeanType.SMALL), Color.WHITE);
        terminateBtn.setOnAction(e -> run(onTerminate));
        VBox buttons = new VBox(10, resumeBtn, saveBtn, terminateBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox content = new VBox(18, title, buttons);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(26, 30, 24, 30));

        Button close = makeCloseButton(p);
        close.setOnAction(e -> run(onResume)); // × 同「继续」(BR-05)

        StackPane card = new StackPane(content, close);
        card.setBackground(new Background(new BackgroundFill(p.boardLight(), new CornerRadii(14), Insets.EMPTY)));
        card.setEffect(new DropShadow(20, 0, 6, Color.rgb(0, 0, 0, 0.35)));
        card.setPrefWidth(270);
        card.setMaxWidth(270);
        card.setMaxHeight(Region.USE_PREF_SIZE); // 高度内容自适应,不被遮罩拉伸
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        StackPane.setMargin(close, new Insets(8, 10, 0, 0));
        return card;
    }

    /** 选项按钮:字体/尺寸走 PrimaryButton 默认样式类(primary-button);此处只设色值与交互 */
    private PrimaryButton makeButton(String text, Color bg, Color fg) {
        PrimaryButton btn = new PrimaryButton();
        btn.setText(text);
        btn.setTextFill(fg);
        btn.setBackground(new Background(new BackgroundFill(bg, new CornerRadii(8), Insets.EMPTY)));
        btn.setFocusTraversable(false); // 键位采集在 scene 全局过滤器,不依赖焦点
        return btn;
    }

    /** 右上角关闭按钮 ×(BR-05 第三种恢复方式;字号/内边距走 CSS) */
    private Button makeCloseButton(Palette p) {
        Button close = new Button("×");
        close.getStyleClass().add("overlay-close");
        close.setTextFill(p.snakeTail());
        close.setBackground(Background.EMPTY);
        close.setFocusTraversable(false);
        return close;
    }

    /** 安全执行注入动作(未接线时点击无动作) */
    private static void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}
