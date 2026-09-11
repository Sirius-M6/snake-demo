package view.widgets;

import javafx.scene.control.Button;

/**
 * 主按钮:统一样式与字号(默认样式类 primary-button,字体/尺寸规格见 css/app.css)
 */
public class PrimaryButton extends Button {

    /** 构造:挂默认样式类(样式统一由 app.css 提供) */
    public PrimaryButton() {
        getStyleClass().add("primary-button");
    }
}
