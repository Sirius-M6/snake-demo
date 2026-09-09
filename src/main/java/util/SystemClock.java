package util;

/**
 * 生产时钟:实现 GameClock(生产用)
 */
public class SystemClock implements GameClock {

    /** 返回系统当前毫秒(System.currentTimeMillis()) */
    @Override
    public long nowMs() {
        return 0L;
    }
}
