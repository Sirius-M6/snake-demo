package view.fx;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * 吃豆爆点动画:豆色同心扩散环,播放于吃豆格像素中心;
 * 一次性特效,播完自移除;挂载于 GameView 棋盘叠加层(center StackPane 最上层)
 */
public class BeanBurst extends Pane {

    /** 扩散环数量 */
    private static final int RING_COUNT = 3;

    /** 环初始半径(px) */
    private static final double START_RADIUS = 2;

    /** 最大扩散半径(px;约 1.4 格) */
    private static final double MAX_RADIUS = 30;

    /** 单环扩散时长(ms) */
    private static final double RING_MS = 380;

    /** 环间错峰(ms;依次拉开扩散层次) */
    private static final double STAGGER_MS = 70;

    /** 播放一次爆点:cx/cy = 吃豆格像素中心(调用方按格换算),color = 豆语义色(Palette.beanColor) */
    public void burstAt(double cx, double cy, Color color) {
        Timeline timeline = new Timeline();
        for (int i = 0; i < RING_COUNT; i++) {
            Circle ring = new Circle(cx, cy, START_RADIUS);
            ring.setFill(null);
            ring.setStroke(color);
            ring.setStrokeWidth(2.4 - i * 0.6); // 外环渐细
            ring.setOpacity(0);
            getChildren().add(ring);
            double delay = i * STAGGER_MS;
            timeline.getKeyFrames().addAll(
                    // 错峰到点:显现并保持小半径
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(ring.opacityProperty(), 0.9 - i * 0.2),
                            new KeyValue(ring.radiusProperty(), START_RADIUS)),
                    // 扩散 + 渐隐
                    new KeyFrame(Duration.millis(delay + RING_MS),
                            new KeyValue(ring.radiusProperty(), MAX_RADIUS, Interpolator.EASE_OUT),
                            new KeyValue(ring.opacityProperty(), 0, Interpolator.EASE_OUT)));
        }
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
