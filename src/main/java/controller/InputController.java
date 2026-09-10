package controller;

import model.Direction;

/**
 * 输入判定(C 模块):按键 → 合法转向;0.15 s 窗口/禁掉头/方向颠倒
 */
public class InputController {

    public static final long INPUT_WINDOW_MS = 150L;
    /** 待生效方向(一拍转向缓冲,不排队) */
    private Direction pendingDir;

    /** 上次生效方向与其真实时刻(0.15 s 窗口) */
    private Direction lastDir;
    private long lastAtMs;

    /** 距上次 < 0.15 s 视为连按:仅更新方向不重计时;否则正常入缓冲(只存一拍,不排队) */
    public void queueInput(Direction raw, long realMs) {
        if (lastAtMs > 0 && (realMs - lastAtMs) < INPUT_WINDOW_MS) {
            // 0.15s窗口内，仅更新缓冲，不重置计时
            pendingDir = raw;
        } else {
            pendingDir = raw;
        }
    }

    /** 节拍点消费缓冲:与 headDir 相反 → 保持原向(禁止掉头);inverted → 整体取反;记录 lastDir/lastAtMs */
    public Direction resolveTurn(Direction headDir, boolean inverted) {
        Direction target = pendingDir;
        if(target == null){
            target = lastDir;
        }
        // 禁止直接掉头
        if(target != null && headDir.isOpposite(target)){
            target = headDir;
        }
        Direction finalDir = target;
        if(inverted && finalDir != null){
            finalDir = finalDir.inverted();
        }
        // 更新生效方向与时间戳
        lastDir = finalDir;
        lastAtMs = System.nanoTime() / 1_000_000L;
        pendingDir = null;
        return finalDir;
    }
}
