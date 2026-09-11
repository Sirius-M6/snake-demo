package util;

import java.util.List;
import java.util.Random;

/**
 * 按权重随机抽取
 */
public class WeightedPicker {

    /** 注入的随机源;测试传固定种子 Random(0) 可复现 */
    private final Random random;

    /** 构造:注入 Random(测试可固定种子,AC-18 比例统计可复现) */
    public WeightedPicker(Random random) {
        this.random = random;
    }

    /** 按权重取一(豆类型抽取);items 与 weights 须等长非空,权重均为正 */
    public <T> T pick(List<T> items, List<Integer> weights) {
        if (items == null || weights == null || items.isEmpty() || items.size() != weights.size()) {
            throw new IllegalArgumentException("items 与 weights 必须等长且非空");
        }
        int total = 0;
        for (int w : weights) {
            if (w <= 0) {
                throw new IllegalArgumentException("权重必须为正数: " + w);
            }
            total += w;
        }
        int roll = random.nextInt(total);
        int acc = 0;
        for (int i = 0; i < items.size(); i++) {
            acc += weights.get(i);
            if (roll < acc) {
                return items.get(i);
            }
        }
        return items.get(items.size() - 1); // 理论不可达(防御)
    }

    /** 随机整数(如随机落位索引);bound 必须为正 */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound 必须为正数: " + bound);
        }
        return random.nextInt(bound);
    }
}
