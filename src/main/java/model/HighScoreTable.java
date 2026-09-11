package model;

import java.util.HashMap;
import java.util.Map;

/**
 * 最高分表(按地图统计 mapId → score,不区分难度)
 */
public class HighScoreTable {

    /** 骨架补充(原因:最高分表需内部存储,骨架未声明字段;须在《详细设计说明书》变更记录登记) */
    private final Map<String, Integer> scores = new HashMap<>();

    /** 查询某图最高分(无记录返回 0) */
    public int scoreOf(String mapId) {
        Integer value = scores.get(mapId);
        return value == null ? 0 : value;
    }

    /** 仅更高时覆盖,返回是否新纪录(最高分按地图、不区分难度) */
    public boolean putIfHigher(String mapId, int score) {
        if (score > scoreOf(mapId)) {
            scores.put(mapId, score);
            return true;
        }
        return false;
    }

    /** 全部记录快照(骨架补充,原因:TextCodec.encodeScores 需枚举全部记录,骨架原仅有单点查询;须在《详细设计说明书》变更记录登记) */
    public Map<String, Integer> allScores() {
        return new HashMap<>(scores);
    }
}
