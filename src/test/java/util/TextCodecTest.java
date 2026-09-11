package util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import model.Bean;
import model.BeanType;
import model.Difficulty;
import model.HighScoreTable;
import model.Point;
import model.SaveData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TextCodec 单元测试:存档/最高分编解码往返与容错;
 * 纯文本操作,不碰磁盘(对应《详细设计文档》第 8 章第 1/2 组用例)
 */
class TextCodecTest {

    /** 被测对象(无状态,可共享) */
    private final TextCodec codec = new TextCodec();

    // ===== 存档编解码 =====

    @Test
    @DisplayName("存档全字段往返一致")
    void saveRoundTripKeepsAllFields() {
        SaveData origin = new SaveData();
        origin.mapId = "pipe";
        origin.difficulty = Difficulty.NORMAL;
        origin.score = 7;
        origin.gameTimeMs = 12345L;
        origin.intervalMs = 150L;
        origin.debuffRemainingMs = 800L;
        origin.body = List.of(new Point(3, 5), new Point(3, 6), new Point(3, 7));
        origin.beans = List.of(
                new Bean(BeanType.SMALL, new Point(4, 2), 1250L),
                new Bean(BeanType.GOLD, new Point(9, 9), 3000L));

        SaveData back = codec.decodeSave(codec.encodeSave(origin));

        assertNotNull(back);
        assertEquals("pipe", back.mapId);
        assertEquals(Difficulty.NORMAL, back.difficulty);
        assertEquals(7, back.score);
        assertEquals(12345L, back.gameTimeMs);
        assertEquals(150L, back.intervalMs);
        assertEquals(800L, back.debuffRemainingMs);
        // 蛇身:顺序与坐标逐个核对(头在前)
        assertEquals(3, back.body.size());
        assertEquals(3, back.body.get(0).row);
        assertEquals(5, back.body.get(0).col);
        assertEquals(3, back.body.get(2).row);
        assertEquals(7, back.body.get(2).col);
        // 豆子:类型/坐标/第 4 列(剩余寿命所在位)
        assertEquals(2, back.beans.size());
        assertEquals(BeanType.SMALL, back.beans.get(0).type);
        assertEquals(4, back.beans.get(0).pos.row);
        assertEquals(2, back.beans.get(0).pos.col);
        assertEquals(1250L, back.beans.get(0).bornMs);
        assertEquals(BeanType.GOLD, back.beans.get(1).type);
        assertEquals(3000L, back.beans.get(1).bornMs);
    }

    @Test
    @DisplayName("null/空文本/空白文本 → null")
    void blankTextYieldsNull() {
        assertNull(codec.decodeSave(null));
        assertNull(codec.decodeSave(""));
        assertNull(codec.decodeSave("   "));
    }

    @Test
    @DisplayName("缺 mapId → null(整体无效)")
    void missingMapIdYieldsNull() {
        assertNull(codec.decodeSave("score=3\ngameTimeMs=100\n"));
    }

    @Test
    @DisplayName("未知键忽略,不影响既有字段解析")
    void unknownKeysAreIgnored() {
        SaveData back = codec.decodeSave("mapId=pipe\nversion=99\nfoo=bar\nscore=5\n");
        assertNotNull(back);
        assertEquals("pipe", back.mapId);
        assertEquals(5, back.score);
    }

    @Test
    @DisplayName("空蛇身/空豆列表编解码安全")
    void emptyBodyAndBeansAreSafe() {
        SaveData origin = new SaveData();
        origin.mapId = "pipe";
        origin.difficulty = Difficulty.EASY;
        origin.body = List.of();
        origin.beans = List.of();

        SaveData back = codec.decodeSave(codec.encodeSave(origin));

        assertNotNull(back);
        assertTrue(back.body.isEmpty());
        assertTrue(back.beans.isEmpty());
    }

    // ===== 最高分编解码 =====

    @Test
    @DisplayName("最高分往返一致(多地图)")
    void scoresRoundTrip() {
        HighScoreTable table = new HighScoreTable();
        table.putIfHigher("pipe", 34);
        table.putIfHigher("snow", 12);

        HighScoreTable back = codec.decodeScores(codec.encodeScores(table));

        assertEquals(34, back.scoreOf("pipe"));
        assertEquals(12, back.scoreOf("snow"));
    }

    @Test
    @DisplayName("null/空白文本 → 空表")
    void blankScoresYieldsEmptyTable() {
        assertEquals(0, codec.decodeScores(null).scoreOf("pipe"));
        assertEquals(0, codec.decodeScores("   ").scoreOf("pipe"));
    }

    @Test
    @DisplayName("损坏行跳过,其余记录保留")
    void corruptedScoreLinesAreSkipped() {
        HighScoreTable table = codec.decodeScores("pipe=34\nsnow=abc\n=5\nbad-line\n");
        assertEquals(34, table.scoreOf("pipe"));
        assertEquals(0, table.scoreOf("snow"));
    }

    @Test
    @DisplayName("注释行跳过:无等号与含等号说明均不产生记录")
    void commentLinesAreSkipped() {
        HighScoreTable table = codec.decodeScores(
                "# 贪吃蛇 历史最高分记录(仅保留最高)\npipe=34\n# 格式示例:mapId=最高分\nsnow=12\n");

        assertEquals(34, table.scoreOf("pipe"));
        assertEquals(12, table.scoreOf("snow"));
        assertEquals(2, table.allScores().size()); // 注释行不产生多余记录
    }

    @Test
    @DisplayName("记录文件带注释头,round-trip 完整且头行不污染记录")
    void scoresFileHasHeaderComment() {
        HighScoreTable origin = new HighScoreTable();
        origin.putIfHigher("pipe", 34);

        String text = codec.encodeScores(origin);
        assertTrue(text.startsWith("#"), "首行应为说明性注释");

        HighScoreTable back = codec.decodeScores(text);
        assertEquals(34, back.scoreOf("pipe"));
        assertEquals(1, back.allScores().size());
    }
}
