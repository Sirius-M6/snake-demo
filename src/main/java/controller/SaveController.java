package controller;

import java.util.Optional;
import model.GameState;
import util.Store;
import util.TextCodec;

/**
 * 存档/读档与最高分(B 模块);构造注入 Store(生产 LocalStore / 测试 InMemoryStore)
 */
public class SaveController {

    /** 存储依赖(注入点) */
    private final Store store;

    /** 文本编解码 */
    private final TextCodec codec = new TextCodec();

    /** 构造:注入 Store(生产 LocalStore / 测试 InMemoryStore) */
    public SaveController(Store store) {
        this.store = store;
    }

    /** 是否存在单存档 */
    public boolean hasSave() {
        return false;
    }

    /** 组装配:豆子折算剩余寿命/记 gameTimeMs → SaveData → TextCodec → LocalStore 写入(覆盖旧档) */
    public void saveNow(GameState state) {
    }

    /** 读档重建:蛇身/豆子 bornMs = gameTimeMs − remainingMs;恢复为 PAUSED 由玩家继续 [默认,UI 细节待 QA-11] */
    public Optional<GameState> loadAndResume() {
        return Optional.empty();
    }

    /** 删除存档(如终止游戏时) */
    public void deleteSave() {
    }

    /** 记录页/结算页只读查询:某图最高分 */
    public int highScoreOf(String mapId) {
        return 0;
    }

    /** 高于现纪录则写入并返回 true(结算画面"新纪录"依据) */
    public boolean recordScore(String mapId, int score) {
        return false;
    }
}
