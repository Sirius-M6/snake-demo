package util;

/**
 * 测试时钟:实现 GameClock(测试用),手动拨时间
 */
public class SimulatedClock implements GameClock {

    private long time;

    public SimulatedClock(long initTime) {
        this.time = initTime;
    }
    /** 返回虚拟当前时间 */
    @Override
    public long nowMs() {
        return time;
    }

    /** 手动拨快虚拟时间 → AC-17/AC-18 定时断言 */
    public void advanceMs(long ms) {
        time += ms;
    }
}
