package config;

/**
 * 窗口与场景尺寸(A 模块维护)
 * 字体/配色等视觉值一律走 CSS,不在此重复(依据界面设计)
 */
public final class UiConfig {

    /** HUD 表头区高度(计分/暂停按钮所在):与单格同宽 25 px,棋盘置于其下方 */
    public static final double HUD_H = BoardConfig.CELL_SIZE_PX;

    /** 主舞台宽度 = 棋盘宽 25 px × 20 列(主页面/难度选择页/地图选择页窗口尺寸一致,共用本值) */
    public static final double WINDOW_W = BoardConfig.COLS * BoardConfig.CELL_SIZE_PX;

    /** 主舞台高度 = HUD 25 px + 棋盘 25 px × 20 行 = 25 px × 21,棋盘置于下方 */
    public static final double WINDOW_H = HUD_H + BoardConfig.ROWS * BoardConfig.CELL_SIZE_PX;

    /** 主舞台最小宽度(固定尺寸窗口:最小 = 实际) */
    public static final double MIN_W = WINDOW_W;

    /** 主舞台最小高度(固定尺寸窗口:最小 = 实际) */
    public static final double MIN_H = WINDOW_H;

    /** 私有构造:常量类不可实例化 */
    private UiConfig() {
    }
}
