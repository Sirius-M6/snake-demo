package org.example;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * 程序入口;只做装配与键位采集转发,不含规则(全体维护,PM 协调装配)
 */
public class MainApp extends Application {

    /** JavaFX 启动入口 */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * 依赖装配根与场景装载:
     * SystemClock + 各 config + 四 Controller(持久化注入 Store 的 LocalStore 实现;
     * InMemoryStore 仅测试用,不进装配)→ PageRouter 显示主界面;
     * 方向键/空格/R/Esc 全局键位采集 → 纯数据转 controller
     */
    @Override
    public void start(Stage stage) {
        // 待填充:依赖装配、场景装载与全局键位采集
        stage.setTitle("贪吃蛇");
        stage.show();
    }

    /** 应用退出钩子(如需清理) */
    @Override
    public void stop() {
        // 待填充
    }
}
