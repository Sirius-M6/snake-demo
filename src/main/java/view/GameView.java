package view;

import controller.GameController;
import controller.GameEvents;
import model.Bean;
import model.BeanType;
import model.GameOverReason;
import model.GamePhase;
import model.Point;
import model.ReadOnlyGameState;

/**
 * 游戏主场景(工作量最大):三层 = HUD / Canvas 棋盘 / 特效与弹层;
 * 只收事件转发 + 只读渲染,规则零行;实现 GameEvents
 */
public class GameView implements GameEvents {

    /** 构造:游戏界面;持有 GameController 引用(按键转发 → queueInput / pause / resume / R / Esc) */
    public GameView(GameController controller) {
    }

    /** 启动渲染循环:AnimationTimer 每帧 → GameController.tick(now) → render(state 快照) */
    public void startRenderLoop() {
    }

    /** 单 Canvas 重绘:棋盘格/障碍/五类豆/蛇(主题配色取自 Palette);不维护 400+ 网格节点 */
    public void render(ReadOnlyGameState state) {
    }

    /** 吃豆(效果结算后)→ 吃豆爆点/飘分特效 */
    @Override
    public void onBeanEaten(BeanType type, Point pos) {
    }

    /** 新豆补刷成功 → 新豆出现动画(可选) */
    @Override
    public void onBeanRefilled(Bean bean) {
    }

    /** 方向颠倒剩余变化 → HUD 颠倒横幅/倒计时 */
    @Override
    public void onDebuffChanged(long remainingMs) {
    }

    /** 终局 → 结算画面(最终分/最高分/原因文案;R 重开 / Esc 回主界面) */
    @Override
    public void onGameOver(GameOverReason reason) {
    }

    /** 状态机迁移 → 暂停层显隐等 UI 联动 */
    @Override
    public void onPhaseChanged(GamePhase phase) {
    }
}
