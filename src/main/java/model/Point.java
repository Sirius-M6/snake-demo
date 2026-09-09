package model;

/**
 * 不可变棋盘坐标(基础类型);值比较与打印(row,col)
 */
public final class Point {

    /** 行坐标 */
    public final int row;

    /** 列坐标 */
    public final int col;

    /** 构造:不可变棋盘坐标 */
    public Point(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /** 值比较 */
    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    /** 值哈希 */
    @Override
    public int hashCode() {
        return 0;
    }

    /** 打印 (row,col) */
    @Override
    public String toString() {
        return "";
    }
}
