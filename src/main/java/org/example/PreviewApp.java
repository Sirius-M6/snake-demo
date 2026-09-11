package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.BeanType;
import model.GameOverReason;
import model.GamePhase;
import model.Point;
import view.GameView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 开发预览入口(非正式入口;MainApp 完成装配后可整体删除):独立查看 GameView 桩数据渲染与全部特效/弹层,不依赖 controller;
 * 键盘演示:1 吃小豆(爆点+飘分) / 2 吃毒豆(红闪) / 3 颠倒倒计时 3s / 4 结算弹层 / P 暂停弹层开关 / Esc 退出;
 * --shot:启动后自动截图 preview-shot.png 并退出(无窗口环境验证用)
 */
public class PreviewApp extends Application {

    /** 暂停弹层当前态(P 键切换用) */
    private boolean paused;

    @Override
    public void start(Stage stage) {
        GameView view = new GameView(null); // 无 controller:预览阶段输入过滤与弹层按钮不接线
        Scene scene = new Scene(view, 520, 580);
        // 样式表挂载:正式挂载点在 MainApp 装配时;此处预览入口自行加载
        scene.getStylesheets().add(PreviewApp.class.getResource("/css/app.css").toExternalForm());
        scene.setOnKeyPressed(e -> handleKey(e.getCode(), view));
        stage.setTitle("界面预览:1吃豆 2毒豆 3颠倒 4结算 P暂停 Esc退出");
        stage.setScene(scene);
        stage.show();
        view.render(null); // 单帧渲染桩数据(真状态接入后由渲染循环驱动)
        if (getParameters().getRaw().contains("--shot")) {
            new Timeline(new KeyFrame(Duration.millis(800), e -> {
                snapshot(scene, "preview-shot.png");
                Platform.exit();
            })).play();
        }
    }

    /** 键位 → 直接调 GameEvents 回调(事件语义等价 controller 触发;仅演示,不含规则) */
    private void handleKey(KeyCode code, GameView view) {
        switch (code) {
            case DIGIT1 -> view.onBeanEaten(BeanType.SMALL, new Point(8, 8));
            case DIGIT2 -> view.onBeanEaten(BeanType.POISON, new Point(12, 12));
            case DIGIT3 -> startDebuffDemo(view);
            case DIGIT4 -> view.onGameOver(GameOverReason.SELF_COLLISION);
            case P -> {
                view.onPhaseChanged(paused ? GamePhase.RUNNING : GamePhase.PAUSED);
                paused = !paused;
            }
            case ESCAPE -> Platform.exit();
            default -> {
            }
        }
    }

    /** 3 键:模拟颠倒 debuff 生效 3 秒(HUD 徽章 + 棋盘横幅倒计时,结束自动隐藏) */
    private static void startDebuffDemo(GameView view) {
        long[] remaining = {3000};
        view.onDebuffChanged(remaining[0]);
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            remaining[0] -= 100;
            view.onDebuffChanged(remaining[0]);
        }));
        timeline.setCycleCount(30);
        timeline.play();
    }

    /** 截图存 PNG(Scene 自渲染,不受窗口遮挡影响) */
    private static void snapshot(Scene scene, String file) {
        try {
            WritableImage img = scene.snapshot(null);
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = img.getPixelReader();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    bi.setRGB(x, y, reader.getArgb(x, y));
                }
            }
            ImageIO.write(bi, "png", new File(file));
            System.out.println("shot saved: " + file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** JavaFX 启动入口 */
    public static void main(String[] args) {
        launch(args);
    }
}
