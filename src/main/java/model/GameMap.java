package model;

import java.util.List;

/**
 * 主题地图(初始/管道/雪山/沙滩/田地);障碍布局由 map.md 字符画解析('#'=障碍、'*'=空),碰撞判定只读消费
 */
public class GameMap {

    /** 地图标识(如 "pipe"),供界面与存档引用 */
    public String id;

    /** 展示名(如 "管道"),供界面/记录页展示 */
    public String displayName;

    /** 障碍网格 [row][col],true = 障碍('#'格);行数/列数随字符画而定 */
    private boolean[][] obstacleGrid = new boolean[0][0];

    /** 构造:主题地图(标识 + 展示名;障碍数据由 fromCharMap 填充) */
    public GameMap(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /** 该格是否为障碍(越界格一律视为非障碍) */
    public boolean obstacleAt(Point p) {
        if (p.row < 0 || p.row >= obstacleGrid.length) {
            return false;
        }
        boolean[] row = obstacleGrid[p.row];
        return p.col >= 0 && p.col < row.length && row[p.col];
    }

    /** 工厂:字符画 → 障碍网格(供 MapCatalog 调用;id/展示名由调用方回填) */
    public static GameMap fromCharMap(List<String> charMap) {
        boolean[][] grid = new boolean[charMap.size()][];
        for (int row = 0; row < charMap.size(); row++) {
            String line = charMap.get(row);
            grid[row] = new boolean[line.length()];
            for (int col = 0; col < line.length(); col++) {
                grid[row][col] = line.charAt(col) == '#';
            }
        }
        GameMap map = new GameMap(null, null);
        map.obstacleGrid = grid;
        return map;
    }
}
