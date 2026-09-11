package view.widgets;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.GameOverReason;
import view.Palette;

/**
 * 结算弹层(终局叠层):半透明遮罩 + 居中卡片 —— 原因文案 / 最终得分 / 本图最高分 / 操作提示(BR-51);
 * 数据经 showResult 注入(GameView 在 onGameOver 中调用;最高分待 M2 接 SaveController);
 * R 重开 / Esc 退出由 GameView 键位转发支持;配色取自 Palette,字体/间距走 CSS(app.css)
 */
public class GameOverOverlay extends StackPane {

    /** 结束原因行(文案由 view 映射,枚举注释约定) */
    private final Label reasonLabel = new Label();

    /** 最终得分(大字) */
    private final Label scoreLabel = new Label("0");

    /** 本图历史最高分行 */
    private final Label bestLabel = new Label("本图最高分 0");

    /** 构造:搭遮罩背景 + 居中卡片;初始隐藏(由终局事件显示) */
    public GameOverOverlay() {
        setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.45), CornerRadii.EMPTY, Insets.EMPTY)));
        getChildren().add(buildCard());
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setVisible(false);
        setManaged(false);
    }

    /** 显示结算:原因 + 最终得分 + 本图最高分(BR-51);重复终局时覆盖上次内容 */
    public void showResult(GameOverReason reason, int finalScore, int bestScore) {
        reasonLabel.setText(reasonText(reason));
        scoreLabel.setText(String.valueOf(finalScore));
        bestLabel.setText("本图最高分 " + bestScore);
        setManaged(true);
        setVisible(true);
    }

    /** 隐藏弹层(重开/复位联动) */
    public void hideOverlay() {
        setVisible(false);
        setManaged(false);
    }

    /** 结束原因 → 结算文案(五种:撞自身/撞障碍/分数为负/通关/主动终止) */
    private static String reasonText(GameOverReason reason) {
        return switch (reason) {
            case SELF_COLLISION -> "撞到了自己";
            case OBSTACLE_HIT -> "撞到了障碍";
            case SCORE_NEGATIVE -> "分数为负";
            case CLEARED -> "恭喜通关!";
            case ABANDONED -> "已终止游戏";
        };
    }

    /** 卡片:米白底 + 圆角 + 投影;标题/原因/得分/最高分/提示竖直居中 */
    private StackPane buildCard() {
        Palette p = Palette.of(Palette.Theme.PIPE); // TODO 真状态:随主题取色(与渲染/HUD 一致)
        Label title = new Label("游戏结束");
        title.getStyleClass().add("overlay-title");
        title.setTextFill(p.snakeTail());
        VBox.setMargin(title, new Insets(0, 0, 6, 0));

        reasonLabel.getStyleClass().add("result-reason");
        reasonLabel.setTextFill(p.snakeTail().deriveColor(0, 1, 1, 0.72)); // 次要文字:同色系淡化

        Label scoreCaption = new Label("最终得分");
        scoreCaption.getStyleClass().add("result-dim");
        scoreCaption.setTextFill(p.snakeTail().deriveColor(0, 1, 1, 0.6));
        VBox.setMargin(scoreCaption, new Insets(10, 0, 0, 0));

        scoreLabel.getStyleClass().add("result-score");
        scoreLabel.setTextFill(p.snakeHead()); // 最终分用蛇头亮蓝强调

        bestLabel.getStyleClass().add("result-best");
        bestLabel.setTextFill(p.snakeTail());
        VBox.setMargin(bestLabel, new Insets(4, 0, 0, 0));

        Label hint = new Label("按 R 重新开始 · 按 Esc 退出");
        hint.getStyleClass().add("result-dim");
        hint.setTextFill(p.snakeTail().deriveColor(0, 1, 1, 0.6));
        VBox.setMargin(hint, new Insets(12, 0, 0, 0));

        VBox content = new VBox(6, title, reasonLabel, scoreCaption, scoreLabel, bestLabel, hint);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(26, 34, 22, 34));

        StackPane card = new StackPane(content);
        card.setBackground(new Background(new BackgroundFill(p.boardLight(), new CornerRadii(14), Insets.EMPTY)));
        card.setEffect(new DropShadow(20, 0, 6, Color.rgb(0, 0, 0, 0.35)));
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setMaxHeight(Region.USE_PREF_SIZE); // 高度内容自适应,不被遮罩拉伸
        return card;
    }
}
