package util;

import model.HighScoreTable;
import model.SaveData;

/**
 * 存档/最高分 ↔ 文本编解码(UTF-8 键值行)
 */
public class TextCodec {

    /** 存档 → 文本(UTF-8 键值行),字段随 SaveData 定稿 */
    public String encodeSave(SaveData saveData) {
        return null;
    }

    /** 文本 → 存档 */
    public SaveData decodeSave(String text) {
        return null;
    }

    /** 最高分表 → 文本 */
    public String encodeScores(HighScoreTable table) {
        return null;
    }

    /** 文本 → 最高分表 */
    public HighScoreTable decodeScores(String text) {
        return null;
    }
}
