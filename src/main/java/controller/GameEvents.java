package controller;

import model.Bean;
import model.BeanType;
import model.GameOverReason;
import model.GamePhase;
import model.Point;

/**
 * 事件接口(controller 触发,view 实现,定义于 controller 包);
 * view 通过事件联动 UI,不猜测状态(diff 反模式禁止)
 */
public interface GameEvents {

    /** 吃豆(效果结算后)→ 吃豆爆点/飘分特效 */
    void onBeanEaten(BeanType type, Point pos);

    /** 新豆补刷成功 → 新豆出现动画(可选) */
    void onBeanRefilled(Bean bean);

    /** 方向颠倒剩余变化 → HUD 颠倒横幅/倒计时 */
    void onDebuffChanged(long remainingMs);

    /** 终局 → 结算画面(最终分/最高分/原因文案) */
    void onGameOver(GameOverReason reason);

    /** 状态机迁移 → 暂停层显隐等 UI 联动 */
    void onPhaseChanged(GamePhase phase);
}
