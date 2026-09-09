package model;

import java.util.List;

/**
 * 存档数据载体(纯 DTO,编解码由 util.TextCodec 完成)
 * 豆子存"剩余寿命"而非出生时刻,恢复时折算 bornMs;
 * 补刷"进行中"的延迟计划与积压数默认不入档(恢复后按当前豆子数量自然继续)[待确认:QA-11]
 */
public class SaveData {

    /** 局配置与得分:地图 id / 难度 / 得分 */
    public String mapId;
    public Difficulty difficulty;
    public int score;

    /** 蛇身坐标序列 */
    public List<Point> body;

    /** 豆子:{type, pos, remainingMs}(存剩余寿命而非出生时刻) */
    public List<Bean> beans;

    /** 方向颠倒剩余 */
    public long debuffRemainingMs;

    /** 当前每格耗时(速度档) */
    public long intervalMs;

    /** 存档时游戏时间 */
    public long gameTimeMs;
}
