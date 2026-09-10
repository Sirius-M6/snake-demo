package view.fx;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * 吃豆爆点动画:豆色同心扩散环,播放于吃豆格像素中心;
 * 一次性特效,播完自移除;挂载于 GameView 棋盘叠加层(center StackPane 最上层)
 * 当前阶段:壳(M2 接线:GameView.onBeanEaten → burstAt)
 */
public class BeanBurst extends Pane {

    /** 播放一次爆点:cx/cy = 吃豆格像素中心(调用方按格换算),color = 豆语义色(Palette.beanColor) */
    public void burstAt(double cx, double cy, Color color) {
        // TODO M2:同心扩散环/粒子动画,时长与节奏按 P0 验收微调
    }
}
