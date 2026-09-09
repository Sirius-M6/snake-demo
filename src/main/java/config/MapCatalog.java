package config;

import java.util.List;
import model.GameMap;

/**
 * 主题地图集:管道/雪山字符画数据(A 模块维护地图布局数据)
 * 约定:地图字符画 20 行 × 20 列,'#'=障碍、'.'=空;障碍不占四周边界通道;碰撞判定只读消费
 */
public final class MapCatalog {

    /** 返回主题地图列表:管道/雪山(字符画 '#'=障碍、'.'=空,20×20) */
    public static List<GameMap> defaultMaps() {
        return null;
    }

    /** 按 id 取图,取不到抛 IllegalArgumentException(选择页/存档用) */
    public static GameMap byId(String id) {
        return null;
    }

    /** 私有构造:常量类不可实例化 */
    private MapCatalog() {
    }
}
