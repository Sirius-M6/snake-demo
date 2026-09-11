package view.fx;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * 得分飘字("+6"/"-6"):从吃豆格中心上飘渐隐;毒豆扣分用 DAMAGE_COLOR 红,正面豆用豆本色;
 * 一次性特效,播完自移除;挂载于棋盘叠加层
 */
public class ScorePopup extends Pane {

    /** 扣分警示红(毒豆飘字语义色,跨主题统一) */
    public static final Color DAMAGE_COLOR = Color.web("#C0392B");

    /** 上飘距离(px;约 1 格) */
    private static final double RISE_PX = 26;

    /** 飘字时长(ms) */
    private static final double POPUP_MS = 700;

    /** 飘一条分数文案:cx/cy = 格像素中心;text 由调用方拼符号("+6"/"-10");color 为主色 */
    public void popAt(double cx, double cy, String text, Color color) {
        Text label = new Text(text);
        label.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 17));
        label.setFill(color);
        // 深色投影:任何棋盘底色上保持可读
        label.setEffect(new DropShadow(3, 0, 1, Color.rgb(0, 0, 0, 0.55)));
        // 文本中心对准格心(Text 以基线定位,x 按半宽回退,y 下沉补偿视觉重心)
        label.setX(cx - label.getLayoutBounds().getWidth() / 2);
        label.setY(cy + 6);
        getChildren().add(label);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(POPUP_MS),
                        new KeyValue(label.translateYProperty(), -RISE_PX, Interpolator.EASE_OUT),
                        new KeyValue(label.opacityProperty(), 0, Interpolator.EASE_IN)));
        timeline.setOnFinished(e -> removeSelf());
        timeline.play();
    }

    /** 播完自移除(宿主为叠加层) */
    private void removeSelf() {
        if (getParent() instanceof Pane parent) {
            parent.getChildren().remove(this);
        }
    }
}
