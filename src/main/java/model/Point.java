package model;

import java.util.Objects;

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

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    /** 值比较 */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return row == point.row && col == point.col;
    }
    /** 值哈希 */
    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    /** 打印 (row,col) */
    @Override
    public String toString() {
        return "Point(" + row + "," + col + ")";
    }
}
