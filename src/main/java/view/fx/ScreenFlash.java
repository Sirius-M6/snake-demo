package view.fx;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * 整屏红闪:吃毒/大毒扣分与死亡瞬间的警示(半透明红整层快速渐隐);
 * 单实例复用,重触发重放;挂载于棋盘叠加层最顶层
 */
public class ScreenFlash extends Pane {

    /** 警示红(跨主题统一) */
    private static final Color FLASH_COLOR = Color.web("#E74C3C");

    /** 起始透明度(峰值) */
    private static final double PEAK_OPACITY = 0.45;

    /** 渐隐时长(ms) */
    private static final double FADE_MS = 380;

    /** 全层红蒙版(宽高随宿主跟随) */
    private final Rectangle veil = new Rectangle();

    /** 渐隐时间线(重触发时重放) */
    private final Timeline fade;

    /** 构造:预建红蒙版(初始全透明)与渐隐时间线 */
    public ScreenFlash() {
        veil.setFill(FLASH_COLOR);
        veil.setOpacity(0);
        veil.widthProperty().bind(widthProperty());
        veil.heightProperty().bind(heightProperty());
        getChildren().add(veil);
        setMouseTransparent(true);
        fade = new Timeline(new KeyFrame(Duration.millis(FADE_MS),
                new KeyValue(veil.opacityProperty(), 0, Interpolator.EASE_OUT)));
    }

    /** 触发一次红闪(进行中重触发则重放;当前不区分强度) */
    public void flash() {
        fade.stop();
        veil.setOpacity(PEAK_OPACITY);
        fade.play();
    }
}
