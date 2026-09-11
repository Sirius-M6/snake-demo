package config;

import java.util.List;
import model.Point;

/**
 * 棋盘几何常量:20×20、25 px、开局蛇位
 */
public final class BoardConfig {

    /** 行数 */
    public static final int ROWS = 20;

    /** 列数(棋盘 20×20 格) */
    public static final int COLS = 20;

    /** 单格 25×25 px(棋盘绘制区 500×500 px,HUD 另占表头区) */
    public static final int CELL_SIZE_PX = 25;

    /** 开局蛇身初始坐标序列(头在前:棋盘中央 3 节、头在最右、开局向右) */
    public static List<Point> snakeInit() {
        // 棋盘中心位置，三节蛇，向右，蛇头在最右
        int mid = ROWS / 2;
        // 2026-09-11 集成修正:返回序改为头在前(原序经"头方向反推"得 LEFT,开局朝向反向,按→键被禁掉头吞掉)
        return List.of(
                new Point(mid, mid),
                new Point(mid, mid - 1),
                new Point(mid, mid - 2)
        );
    }

    /** 私有构造:常量类不可实例化 */
    private BoardConfig() {
    }
}
