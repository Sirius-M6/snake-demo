package view.widgets;

import javafx.scene.Cursor;
import javafx.scene.control.Button;

/**
 * 主按钮:已有界面主按钮的统一样式与字号集中于此;
 * 淡橙底 + 焦糖棕加粗文字 + 圆润字体(华文彩云回退微软雅黑),悬停/选中背景色由各界面传入;
 * 只统一样式与字号,尺寸(240×44/72×36 等)仍由各界面 setPrefSize 决定,不改变已有布局
 */
public class PrimaryButton extends Button {

    /** 按钮按键颜色(淡橙,主界面/选择页/记录页共用) */
    public static final String COLOR_BUTTON = "#FDE6BF";

    /** 按钮文字色(焦糖棕):清新可爱,替代沉闷的默认黑 */
    public static final String COLOR_TEXT_BUTTON = "#8D6E63";

    /** 可爱圆润字体:优先华文彩云,缺省回退微软雅黑 */
    public static final String FONT_ROUND = "-fx-font-family: 'ST Caiyun', '华文彩云', 'Microsoft YaHei', '微软雅黑';";

    /** 按钮字号/字重(加粗)/文字色,基础与悬停样式共用 */
    private static final String BUTTON_FONT_STYLE =
            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_BUTTON + "; " + FONT_ROUND;

    /** 无参构造:套用统一样式(淡橙底 + 加粗圆润字体),界面可再 setPrefSize 与覆盖样式 */
    public PrimaryButton() {
        applyUnifiedStyle();
    }

    /** 文字构造:套用统一样式(淡橙底 + 加粗圆润字体),界面可再 setPrefSize 与覆盖样式 */
    public PrimaryButton(String text) {
        super(text);
        applyUnifiedStyle();
    }

    /**
     * 按背景色生成按钮样式串:背景 + 加粗字号(基础与悬停样式共用);
     * 已有界面通过它统一 18px 加粗圆润字体,不改变原有布局
     */
    public static String styleWith(String backgroundColor) {
        return "-fx-background-color: " + backgroundColor + "; " + BUTTON_FONT_STYLE;
    }

    /** 按背景色 + 文字色生成样式串(选中态用:后置 text-fill 覆盖基础焦糖棕) */
    public static String styleWith(String backgroundColor, String textColor) {
        return styleWith(backgroundColor) + " -fx-text-fill: " + textColor + ";";
    }

    /** 套用统一样式:淡橙底 + 手型光标,与已有界面主按钮视觉一致 */
    private void applyUnifiedStyle() {
        setStyle(styleWith(COLOR_BUTTON));
        setCursor(Cursor.HAND);
    }
}
