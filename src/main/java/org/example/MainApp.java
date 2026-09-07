package org.example;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("Hello, JavaFX！");

        // 启动按钮
        Button startButton = new Button("启动");
        startButton.setOnAction(e -> label.setText("游戏已启动！"));

        // 关闭按钮：点击后关闭整个窗口
        Button closeButton = new Button("关闭");
        closeButton.setOnAction(e -> stage.close());

        // 两个按钮水平并排
        HBox buttonBox = new HBox(20, startButton, closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(20, label, buttonBox);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 400, 300);
        stage.setTitle("我的第一个 JavaFX 程序");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
