package controller;

import static org.junit.jupiter.api.Assertions.*;

import config.SaveConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.Optional;
import model.Bean;
import model.BeanType;
import model.Difficulty;
import model.GameMap;
import model.GamePhase;
import model.GameState;
import model.Point;
import model.SaveData;
import model.Snake;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.InMemoryStore;
import util.TextCodec;

/**
 * SaveController 单元测试:存档生命周期/组装配/最高分判定;
 * 全部注入 InMemoryStore,不碰磁盘(对应《详细设计文档》第 8 章第 4/5 组用例);
 * 每个用例独立实例 → 独立的 store,用例间互不串扰
 */
class SaveControllerTest {

    /** 内存存储(单测不碰磁盘) */
    private final InMemoryStore store = new InMemoryStore();

    /** 被测对象:注入内存实现 */
    private final SaveController controller = new SaveController(store);

    // ===== 存档生命周期 =====

    @Test
    @DisplayName("hasSave 生命周期:无档→保存→有档→删除→无档(幂等)")
    void hasSaveLifecycle() {
        assertFalse(controller.hasSave());
        controller.saveNow(sampleState("pipe", 3));
        assertTrue(controller.hasSave());
        controller.deleteSave();
        assertFalse(controller.hasSave());
        controller.deleteSave(); // 幂等:重复删除不抛异常
        assertFalse(controller.hasSave());
    }

    @Test
    @DisplayName("无档读档返回空")
    void loadWithoutSaveReturnsEmpty() {
        assertTrue(controller.loadAndResume().isEmpty());
    }

    @Test
    @DisplayName("保存组装配:落盘文本可解码且字段一致")
    void saveWritesDecodableText() {
        controller.saveNow(sampleState("pipe", 3));

        String text = store.loadText(savePath()).orElseThrow();
        SaveData back = new TextCodec().decodeSave(text);

        assertNotNull(back);
        assertEquals("pipe", back.mapId);
        assertEquals(Difficulty.NORMAL, back.difficulty);
        assertEquals(3, back.score);
        assertEquals(2500L, back.gameTimeMs);
        assertEquals(150L, back.intervalMs);
        assertEquals(1, back.beans.size());
    }

    @Test
    @DisplayName("单存档:重复保存覆盖旧档(仅保留最后一次)")
    void saveOverwritesPrevious() {
        controller.saveNow(sampleState("pipe", 3));
        controller.saveNow(sampleState("snow", 7));

        SaveData back = new TextCodec().decodeSave(store.loadText(savePath()).orElseThrow());

        assertNotNull(back);
        assertEquals("snow", back.mapId);
        assertEquals(7, back.score);
    }

    // ===== 读档容错 =====

    @Test
    @DisplayName("损坏存档 → 按无档处理,不抛异常")
    void corruptedSaveLoadsEmpty() {
        store.saveText(savePath(), "mapId=pipe\nscore=abc\n"); // score 非法 → 解析异常被兜住
        assertTrue(controller.loadAndResume().isEmpty());
    }

    @Test
    @DisplayName("地图不存在/未收录 → 旧档作废返回空")
    void missingMapLoadsEmpty() {
        store.saveText(savePath(), "mapId=unknown-map\ndifficulty=EASY\nscore=1\n");
        assertTrue(controller.loadAndResume().isEmpty());
    }

    @Test
    @Disabled("待 A2 实现 MapCatalog.byId、C2 实现 Snake.body/step 后启用(联调阶段)")
    @DisplayName("读档成功:重建 PAUSED 局面")
    void loadRestoresPausedState() {
        GameState state = sampleState("pipe", 3);
        // 折算公式回归(2026-09-11 修正):毒豆出生 0/时限 3000,存档时刻 2500 ⇒ 存档剩余 500,恢复后应仍为 500
        state.beans.add(new Bean(BeanType.POISON, new Point(5, 5), 0L));
        controller.saveNow(state);

        Optional<GameState> loaded = controller.loadAndResume();

        assertTrue(loaded.isPresent());
        GameState restored = loaded.get();
        assertEquals(GamePhase.PAUSED, restored.phase);
        assertEquals(3, restored.score);
        assertEquals(2500L, restored.gameTimeMs);
        // 豆子折算:非小豆剩余寿命与存档时一致;小豆哨兵(不限时)恢复后仍为 MAX
        Bean poison = restored.beans.stream().filter(b -> b.type == BeanType.POISON).findFirst().orElseThrow();
        assertEquals(500L, poison.remainingMs(restored.gameTimeMs));
        Bean small = restored.beans.stream().filter(b -> b.type == BeanType.SMALL).findFirst().orElseThrow();
        assertEquals(Long.MAX_VALUE, small.remainingMs(restored.gameTimeMs));
    }

    // ===== 最高分判定 =====

    @Test
    @DisplayName("recordScore 仅更高时落盘,按地图隔离")
    void recordScoreJudgesNewRecord() {
        assertEquals(0, controller.highScoreOf("pipe"));
        assertTrue(controller.recordScore("pipe", 10));
        assertFalse(controller.recordScore("pipe", 5));
        assertTrue(controller.recordScore("pipe", 12));
        assertEquals(12, controller.highScoreOf("pipe"));
        assertEquals(0, controller.highScoreOf("snow")); // 其他地图不受影响
    }

    @Test
    @DisplayName("isFreshRecord:标记最近一次结算的新纪录,未破清除,迁移到最新图")
    void freshRecordTracksLatestResult() {
        assertFalse(controller.isFreshRecord("pipe")); // 初始:无标记

        assertTrue(controller.recordScore("pipe", 10));
        assertTrue(controller.isFreshRecord("pipe")); // 刚破纪录:点亮

        assertTrue(controller.recordScore("snow", 5));
        assertTrue(controller.isFreshRecord("snow"));
        assertFalse(controller.isFreshRecord("pipe")); // 标记迁移到最近破纪录的图

        assertFalse(controller.recordScore("snow", 2));
        assertFalse(controller.isFreshRecord("snow")); // 最近一局未破:标记清除

        assertFalse(controller.isFreshRecord(null)); // null 安全
    }

    // ===== 辅助 =====

    /** 存档文件路径(与 SaveController 内部一致,经 SaveConfig 拼装) */
    private String savePath() {
        return SaveConfig.SAVE_DIR + File.separator + SaveConfig.SAVE_FILE;
    }

    /** 构造一个可保存的最小局面(snake 非空,避免保存时空指针) */
    private GameState sampleState(String mapId, int score) {
        GameState state = new GameState();
        state.map = new GameMap(mapId, "测试图");
        state.difficulty = Difficulty.NORMAL;
        state.score = score;
        state.snake = new Snake();
        state.beans = new ArrayList<>();
        state.beans.add(new Bean(BeanType.SMALL, new Point(4, 2), 0L));
        state.gameTimeMs = 2500L;
        state.intervalMs = 150L;
        state.debuffRemainingMs = 0L;
        return state;
    }
}
