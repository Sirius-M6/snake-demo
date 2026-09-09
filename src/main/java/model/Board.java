package model;

import java.util.Set;

/**
 * 棋盘:界内/穿墙环化/障碍/空格枚举(几何与占位查询,不含决策)
 */
public class Board {

    /** 坐标是否在界内 */
    public boolean inside(Point p) {
        return false;
    }

    /** 穿墙环化:越界坐标映射到对侧对应位置(依据穿墙规则) */
    public Point wrap(Point p) {
        return null;
    }

    /** 该格是否为障碍 */
    public boolean obstacleAt(Point p) {
        return false;
    }

    /** 可刷新空格列表(排除障碍与传入占用),供豆子定位 */
    public Set<Point> freeCells(Set<Point> occupied) {
        return null;
    }

    /** 可刷新空格计数(空格 < 10 判定依据) */
    public int countFree(Set<Point> occupied) {
        return 0;
    }
}
