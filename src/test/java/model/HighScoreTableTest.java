package model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HighScoreTable 单元测试:按地图、仅更高覆盖(对应《详细设计文档》第 8 章第 3 组用例)
 */
class HighScoreTableTest {

    @Test
    @DisplayName("无记录返回 0")
    void emptyReturnsZero() {
        assertEquals(0, new HighScoreTable().scoreOf("pipe"));
    }

    @Test
    @DisplayName("仅严格更高时覆盖并返回新纪录标识")
    void onlyHigherOverwrites() {
        HighScoreTable table = new HighScoreTable();
        assertTrue(table.putIfHigher("pipe", 10));
        assertEquals(10, table.scoreOf("pipe"));
        assertFalse(table.putIfHigher("pipe", 5));
        assertEquals(10, table.scoreOf("pipe"));
        assertFalse(table.putIfHigher("pipe", 10)); // 相等不算新纪录
        assertTrue(table.putIfHigher("pipe", 12));
        assertEquals(12, table.scoreOf("pipe"));
    }

    @Test
    @DisplayName("按地图相互隔离")
    void scoresAreIsolatedPerMap() {
        HighScoreTable table = new HighScoreTable();
        table.putIfHigher("pipe", 10);
        assertEquals(0, table.scoreOf("snow"));
        table.putIfHigher("snow", 3);
        assertEquals(10, table.scoreOf("pipe"));
        assertEquals(3, table.scoreOf("snow"));
    }

    @Test
    @DisplayName("0 分与负分不建档")
    void zeroAndNegativeAreNotRecorded() {
        HighScoreTable table = new HighScoreTable();
        assertFalse(table.putIfHigher("pipe", 0));
        assertFalse(table.putIfHigher("pipe", -5));
        assertEquals(0, table.scoreOf("pipe"));
    }

    @Test
    @DisplayName("allScores 返回快照,改动不回写")
    void allScoresReturnsSnapshot() {
        HighScoreTable table = new HighScoreTable();
        table.putIfHigher("pipe", 10);
        Map<String, Integer> snapshot = table.allScores();
        snapshot.put("pipe", 999);
        assertEquals(10, table.scoreOf("pipe"));
    }
}
