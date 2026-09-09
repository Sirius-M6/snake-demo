package model;

/**
 * 最高分表(按地图统计 mapId → score,不区分难度)
 */
public class HighScoreTable {

    /** 查询某图最高分(无记录返回 0) */
    public int scoreOf(String mapId) {
        return 0;
    }

    /** 仅更高时覆盖,返回是否新纪录(最高分按地图、不区分难度) */
    public boolean putIfHigher(String mapId, int score) {
        return false;
    }
}
