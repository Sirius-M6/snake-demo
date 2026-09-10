package view.fx;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * 得分飘字("+6"/"-6"):从吃豆格中心上飘渐隐;毒豆扣分红色,正面豆加分亮色(M2 定);
 * 一次性特效,播完自移除;挂载于棋盘叠加层
 * 当前阶段:壳(M2 接线:GameView.onBeanEaten → popAt)
 */
public class ScorePopup extends Pane {

    /** 飘一条分数文案:cx/cy = 格像素中心;text 由调用方拼符号("+6"/"-10");color 为主色 */
    public void popAt(double cx, double cy, String text, Color color) {
        // TODO M2:Text 上飘 + 渐隐动画
    }
}
