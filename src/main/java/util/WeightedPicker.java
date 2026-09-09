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

    /** 按权重取一(豆类型抽取) */
    public <T> T pick(List<T> items, List<Integer> weights) {
        return null;
    }

    /** 随机整数(金豆消除 2~3 节等随机决策) */
    public int nextInt(int bound) {
        return 0;
    }
}
