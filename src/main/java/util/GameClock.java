package util;

/**
 * 时间源抽象(接口);
 * 不包含任何 Timer/Thread 倒计时器,全部"时长"由 controller 以注入时钟被动判定,
 * 保证暂停冻结与可测性
 */
public interface GameClock {

    /** 返回当前时间毫秒 */
    long nowMs();
}
