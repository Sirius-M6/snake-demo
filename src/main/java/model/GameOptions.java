package model;

/**
 * 主界面选择保持(难度 + 地图,跨页面、跨局保持,FP-02)
 */
public class GameOptions {

    /** 难度选择保持(默认 EASY 低) */
    private Difficulty difficulty;

    /** 地图选择保持(默认 MapCatalog 第一张图) */
    private GameMap map;

    /** 读:当前难度 */
    public Difficulty getDifficulty() {
        return null;
    }

    /** 写:设置难度 */
    public void setDifficulty(Difficulty difficulty) {
    }

    /** 读:当前地图 */
    public GameMap getMap() {
        return null;
    }

    /** 写:设置地图 */
    public void setMap(GameMap map) {
    }
}
