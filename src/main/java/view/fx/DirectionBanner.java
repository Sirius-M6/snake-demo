package view.fx;

import javafx.scene.layout.Pane;

/**
 * 方向颠倒横幅 + 实时倒计时:debuff 生效期常驻展示("方向已颠倒,剩余 xx.xs"),结束隐藏;
 * 区别于一次性特效,生命周期由 GameEvents.onDebuffChanged 驱动;
 * 当前阶段:壳(M2 接线:GameView.onDebuffChanged → update;挂 HUD 或棋盘顶由 M2 定)
 */
public class DirectionBanner extends Pane {

    /** 刷新倒计时:remainingMs &gt; 0 显示并更新文案;== 0 隐藏 */
    public void update(long remainingMs) {
        // TODO M2:横幅显隐 + 秒数刷新
    }
}
