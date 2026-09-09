package model;

import java.util.Deque;
import java.util.List;

/**
 * 蛇链,内部 Deque&lt;Point&gt;(头在前)
 */
public class Snake {

    /** 蛇身坐标链(头在前) */
    private Deque<Point> bodyDeque;

    /** 头坐标(移动基准) */
    public Point head() {
        return null;
    }

    /** 当前节数 */
    public int length() {
        return 0;
    }

    /** 蛇身是否占据该格(撞自身判定) */
    public boolean contains(Point p) {
        return false;
    }

    /** 头前进;grow=false 去尾(=普通走格),grow=true 保留尾(=长一节) */
    public void step(Point newHead, boolean grow) {
    }

    /** 从尾消除 n 节(金豆效果;下限 1 节由调用方保证) */
    public void shrink(int n) {
    }

    /** 身体坐标只读快照(渲染/存档) */
    public List<Point> body() {
        return null;
    }

    /** 深拷贝(存档快照) */
    public Snake copy() {
        return null;
    }
}
