package model;

import java.util.HashSet;
import java.util.Set;

/**
 * 棋盘:界内/穿墙环化/障碍/空格枚举(几何与占位查询,不含决策)
 */
public class Board {
    private final int height;
    private final int width;
    private final Set<Point> obstacles;

    public Board(int height, int width, Set<Point> obstacles) {
        this.height = height;
        this.width = width;
        this.obstacles = new HashSet<>(obstacles);
    }

    /** 坐标是否在界内 */
    public boolean inside(Point p) {
        return p.row >= 0 && p.row < height && p.col >= 0 && p.col < width;
    }

    /** 穿墙环化:越界坐标映射到对侧对应位置(依据穿墙规则) */
    public Point wrap(Point p) {
        int r = ((p.row % height) + height) % height;
        int c = ((p.col % width) + width) % width;
        return new Point(r, c);
    }

    /** 该格是否为障碍 */
    public boolean obstacleAt(Point p) {
        return obstacles.contains(p);
    }

    /** 可刷新空格列表(排除障碍与传入占用),供豆子定位 */
    public Set<Point> freeCells(Set<Point> occupied) {
        Set<Point> free = new HashSet<>();
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Point p = new Point(r, c);
                if (!obstacles.contains(p) && !occupied.contains(p)) {
                    free.add(p);
                }
            }
        }
        return free;
    }

    /** 可刷新空格计数(空格 < 10 判定依据) */
    public int countFree(Set<Point> occupied) {
        return freeCells(occupied).size();
    }
}