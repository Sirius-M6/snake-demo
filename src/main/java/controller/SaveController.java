package controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import config.MapCatalog;
import config.SaveConfig;
import model.Bean;
import model.GameMap;
import model.GamePhase;
import model.GameState;
import model.HighScoreTable;
import model.SaveData;
import model.Snake;
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

    /** 最近一次结算产生新纪录的图 id(记录页 [NEW] 标记依据;内存态,不持久化,重启即清) */
    private String freshRecordMapId;

    /** 构造:注入 Store(生产 LocalStore / 测试 InMemoryStore) */
    public SaveController(Store store) {
        this.store = store;
    }

    /** 是否存在单存档 */
    public boolean hasSave() {
        return store.exists(SaveConfig.SAVE_DIR + File.separator + SaveConfig.SAVE_FILE);
    }

    /** 组装配:豆子折算剩余寿命/记 gameTimeMs → SaveData → TextCodec → LocalStore 写入(覆盖旧档) */
    public void saveNow(GameState state) {
        SaveData data = new SaveData();
        data.mapId = state.map.id;
        data.difficulty = state.difficulty;
        data.score = state.score;
        data.body = state.snake.body();
        // 豆子折算剩余寿命:按 SaveData 约定,Bean.bornMs 位在此承载"剩余寿命"(恢复时再折算回 bornMs)
        List<Bean> beans = new ArrayList<>();
        if (state.beans != null) {
            for (Bean b : state.beans) {
                beans.add(new Bean(b.type, b.pos, b.remainingMs(state.gameTimeMs)));
            }
        }
        data.beans = beans;
        data.debuffRemainingMs = state.debuffRemainingMs;
        data.intervalMs = state.intervalMs;
        data.gameTimeMs = state.gameTimeMs;
        String path = SaveConfig.SAVE_DIR + File.separator + SaveConfig.SAVE_FILE;
        store.saveText(path, codec.encodeSave(data));
    }

    /** 读档重建:蛇身/豆子 bornMs = gameTimeMs − remainingMs;恢复为 PAUSED 由玩家继续 [默认,UI 细节待 QA-11] */
    public Optional<GameState> loadAndResume() {
        Optional<String> text = store.loadText(SaveConfig.SAVE_DIR + File.separator + SaveConfig.SAVE_FILE);
        if (text.isEmpty()) {
            return Optional.empty();
        }
        SaveData data;
        try {
            data = codec.decodeSave(text.get());
        } catch (RuntimeException ignored) {
            return Optional.empty(); // 存档损坏:按无存档处理,避免读档崩溃
        }
        if (data == null) {
            return Optional.empty();
        }
        GameMap map;
        try {
            map = MapCatalog.byId(data.mapId);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty(); // 地图已不存在(如地图集调整):旧档作废
        }
        if (map == null) {
            return Optional.empty(); // 防御:地图集未收录该 id 时同样按旧档作废处理
        }
        GameState state = new GameState();
        state.map = map;
        state.difficulty = data.difficulty;
        state.score = data.score;
        // 蛇身重建:复用 Snake.step(_, true)"保留尾"的语义,自尾向头逐节压入即可还原整链(骨架零变更)
        Snake snake = new Snake();
        if (data.body != null) {
            for (int i = data.body.size() - 1; i >= 0; i--) {
                snake.step(data.body.get(i), true);
            }
        }
        state.snake = snake;
        // 豆子恢复:bornMs = 存档 gameTimeMs − 剩余寿命 ⇒ 在恢复后的时间轴上保持原剩余寿命
        List<Bean> beans = new ArrayList<>();
        if (data.beans != null) {
            for (Bean b : data.beans) {
                beans.add(new Bean(b.type, b.pos, data.gameTimeMs - b.bornMs));
            }
        }
        state.beans = beans;
        state.debuffRemainingMs = data.debuffRemainingMs;
        state.intervalMs = data.intervalMs;
        state.gameTimeMs = data.gameTimeMs;
        // controller 为本局唯一修改方:直接置字段(语义等价 setPhase)
        state.phase = GamePhase.PAUSED;
        return Optional.of(state);
    }

    /** 删除存档(如终止游戏时) */
    public void deleteSave() {
        store.delete(SaveConfig.SAVE_DIR + File.separator + SaveConfig.SAVE_FILE);
    }

    /** 记录页/结算页只读查询:某图最高分 */
    public int highScoreOf(String mapId) {
        Optional<String> text = store.loadText(SaveConfig.SAVE_DIR + File.separator + SaveConfig.HIGH_SCORE_FILE);
        if (text.isEmpty()) {
            return 0;
        }
        return codec.decodeScores(text.get()).scoreOf(mapId);
    }

    /** 高于现纪录则写入并返回 true(结算画面"新纪录"依据);同时刷新记录页 [NEW] 标记 */
    public boolean recordScore(String mapId, int score) {
        String path = SaveConfig.SAVE_DIR + File.separator + SaveConfig.HIGH_SCORE_FILE;
        HighScoreTable table = codec.decodeScores(store.loadText(path).orElse(null));
        boolean newRecord = table.putIfHigher(mapId, score);
        // [NEW] 语义:最近一次结算破纪录 -> 标记该图;未破 -> 清除标记(其余图不再标)
        freshRecordMapId = newRecord ? mapId : null;
        if (newRecord) {
            store.saveText(path, codec.encodeScores(table));
        }
        return newRecord;
    }

    /** 记录页 [NEW] 标记:该图是否为最近一次结算产生的新纪录(内存态,重启后不显示);
     *  骨架补充(原因:记录页需"最近新纪录"展示依据,骨架未声明;须在《详细设计说明书》变更记录登记) */
    public boolean isFreshRecord(String mapId) {
        return mapId != null && mapId.equals(freshRecordMapId);
    }
}
