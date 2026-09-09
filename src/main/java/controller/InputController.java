package controller;

import model.Direction;

/**
 * 输入判定(C 模块):按键 → 合法转向;0.15 s 窗口/禁掉头/方向颠倒
 */
public class InputController {

    /** 待生效方向(一拍转向缓冲,不排队) */
    private Direction pendingDir;

    /** 上次生效方向与其真实时刻(0.15 s 窗口) */
    private Direction lastDir;
    private long lastAtMs;

    /** 距上次 < 0.15 s 视为连按:仅更新方向不重计时;否则正常入缓冲(只存一拍,不排队) */
    public void queueInput(Direction raw, long realMs) {
    }

    /** 节拍点消费缓冲:与 headDir 相反 → 保持原向(禁止掉头);inverted → 整体取反;记录 lastDir/lastAtMs */
    public Direction resolveTurn(Direction headDir, boolean inverted) {
        return null;
    }
}
