package controller;

import java.util.List;
import model.Bean;

/**
 * 豆子调度状态机(B 模块,全场最复杂);实现 BR-25/BR-28/BR-29
 * 补刷不记忆原空位类型(延迟/积压的空位一律按抽取定类)
 */
public class BeanController {

    /** 延迟补刷到期游戏时刻(可多个并存) */
    private List<Long> refillDueAt;

    /** 只剩小豆子时的积压空位数(豆子未落地,不入档) */
    private int backlog;

    /** 首批 3 颗:第 1 颗必为小豆子,其余按全局比例抽取;逐颗满足大/金互斥与空位;bornMs = gameTime(0) */
    public void spawnInitial() {
    }

    /** 小豆被吃 → 立即补 1 颗小豆(backlog>0 时再补 1 颗并 --);非小豆被吃 → 有其他非小豆则登记延迟补刷,否则 backlog++ */
    public void onBeanEaten(Bean bean, long gameTime) {
    }

    /** ① 遍历场上豆:寿命 ≤ 0 → 移除并走"非小豆消失"分支;② 到期计划逐一 spawnOne() */
    public void tick(long gameTime) {
    }

    /** 保底:补刷前无小豆 → 必补小豆;否则按正:负(2:1,待 QA-19)定池 → 池内按 3:1:1 或 3:1 抽;互斥冲突剔除;空格<10 抽中 debuff → 改抽正面;落位随机空格 */
    public void spawnOne() {
    }
}
