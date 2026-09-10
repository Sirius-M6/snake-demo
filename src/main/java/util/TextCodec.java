package util;

import java.util.ArrayList;
import java.util.Map;
import model.Bean;
import model.BeanType;
import model.Difficulty;
import model.HighScoreTable;
import model.Point;
import model.SaveData;

/**
 * 存档/最高分 ↔ 文本编解码(UTF-8 键值行)
 */
public class TextCodec {

    /** 存档 → 文本(UTF-8 键值行),字段随 SaveData 定稿 */
    public String encodeSave(SaveData saveData) {
        StringBuilder sb = new StringBuilder();
        sb.append("mapId=").append(saveData.mapId).append('\n');
        sb.append("difficulty=").append(saveData.difficulty).append('\n');
        sb.append("score=").append(saveData.score).append('\n');
        sb.append("gameTimeMs=").append(saveData.gameTimeMs).append('\n');
        sb.append("intervalMs=").append(saveData.intervalMs).append('\n');
        sb.append("debuffRemainingMs=").append(saveData.debuffRemainingMs).append('\n');
        // 蛇身:头在前,形如 "row,col;row,col;…"
        StringBuilder body = new StringBuilder();
        if (saveData.body != null) {
            for (int i = 0; i < saveData.body.size(); i++) {
                Point p = saveData.body.get(i);
                if (i > 0) {
                    body.append(';');
                }
                body.append(p.row).append(',').append(p.col);
            }
        }
        sb.append("body=").append(body).append('\n');
        // 豆子:每颗一行 "bean=类别,r,c,剩余寿命";第 4 列存于 Bean.bornMs 位,由 SaveController 折算填入
        if (saveData.beans != null) {
            for (Bean b : saveData.beans) {
                sb.append("bean=").append(b.type).append(',')
                        .append(b.pos.row).append(',').append(b.pos.col).append(',')
                        .append(b.bornMs).append('\n');
            }
        }
        return sb.toString();
    }

    /** 文本 → 存档(整体无效/缺 mapId 返回 null;未知键忽略以便后续扩展) */
    public SaveData decodeSave(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        SaveData saveData = new SaveData();
        saveData.body = new ArrayList<>();
        saveData.beans = new ArrayList<>();
        for (String line : text.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            switch (key) {
                case "mapId":
                    saveData.mapId = value;
                    break;
                case "difficulty":
                    saveData.difficulty = Difficulty.valueOf(value);
                    break;
                case "score":
                    saveData.score = Integer.parseInt(value);
                    break;
                case "gameTimeMs":
                    saveData.gameTimeMs = Long.parseLong(value);
                    break;
                case "intervalMs":
                    saveData.intervalMs = Long.parseLong(value);
                    break;
                case "debuffRemainingMs":
                    saveData.debuffRemainingMs = Long.parseLong(value);
                    break;
                case "body": {
                    if (!value.isEmpty()) {
                        for (String seg : value.split(";")) {
                            String[] rc = seg.split(",");
                            if (rc.length == 2) {
                                saveData.body.add(new Point(Integer.parseInt(rc[0].trim()), Integer.parseInt(rc[1].trim())));
                            }
                        }
                    }
                    break;
                }
                case "bean": {
                    String[] parts = value.split(",");
                    if (parts.length == 4) {
                        // 形如 "SMALL,4,2,1250":第 4 列为剩余寿命(存于 bornMs 位)
                        saveData.beans.add(new Bean(BeanType.valueOf(parts[0].trim()),
                                new Point(Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())),
                                Long.parseLong(parts[3].trim())));
                    }
                    break;
                }
                default:
                    break; // 未知键忽略
            }
        }
        return saveData.mapId == null ? null : saveData;
    }

    /** 最高分表 → 文本(每行 "mapId=score") */
    public String encodeScores(HighScoreTable table) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : table.allScores().entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        return sb.toString();
    }

    /** 文本 → 最高分表(空文本返回空表;损坏行跳过,不让单行错误破坏整表) */
    public HighScoreTable decodeScores(String text) {
        HighScoreTable table = new HighScoreTable();
        if (text == null || text.isBlank()) {
            return table;
        }
        for (String line : text.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                table.putIfHigher(line.substring(0, eq).trim(), Integer.parseInt(line.substring(eq + 1).trim()));
            } catch (NumberFormatException ignored) {
                // 跳过损坏行
            }
        }
        return table;
    }
}
