package model;

import java.util.List;

/**
 * 主题地图(管道/雪山);地图布局数据由 A 模块维护,碰撞判定只读消费
 */
public class GameMap {

    /** 地图标识(如 "pipe"),供界面与存档引用 */
    public String id;

    /** 展示名(如 "管道"),供界面/记录页展示 */
    public String displayName;

    /** 构造:主题地图(标识 + 展示名) */
    public GameMap(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /** 该格是否为障碍 */
    public boolean obstacleAt(Point p) {
        return false;
    }

    /** 工厂:字符画 → 障碍集(供 MapCatalog 调用) */
    public static GameMap fromCharMap(List<String> charMap) {
        return null;
    }
}
