package view.fx;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.BeanType;
import view.Palette;

import java.util.Locale;

/**
 * 方向颠倒横幅 + 实时倒计时:debuff 生效期常驻展示("方向已颠倒,剩余 xx.xs"),结束隐藏;
 * 区别于一次性特效,生命周期由 GameEvents.onDebuffChanged 驱动;
 * 挂载于棋盘叠加层,由 GameView 顶部居中对齐;尺寸随文案自适应
 */
public class DirectionBanner extends Pane {

    /** 倒计时标签(胶囊底,内容自适应) */
    private final Label label = new Label();

    /** 构造:构建胶囊横幅(大毒豆深紫底,BR-23 触发源联动),初始隐藏 */
    public DirectionBanner() {
        Palette p = Palette.of(Palette.Theme.PIPE); // TODO 真状态:随主题取色
        label.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 14));
        label.setTextFill(Color.WHITE);
        label.setBackground(new Background(new BackgroundFill(
                p.beanColor(BeanType.BIG_POISON), new CornerRadii(13), Insets.EMPTY)));
        label.setPadding(new Insets(4, 14, 4, 14));
        getChildren().add(label);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); // 不被父层拉伸(顶部居中时保持内容尺寸)
        setMouseTransparent(true);
        setVisible(false);
        setManaged(false);
    }

    /** 刷新倒计时:remainingMs > 0 显示并更新文案;== 0 隐藏 */
    public void update(long remainingMs) {
        if (remainingMs > 0) {
            label.setText(String.format(Locale.ROOT, "方向已颠倒,剩余 %.1fs", remainingMs / 1000.0));
            setVisible(true);
            setManaged(true);
        } else {
            setVisible(false);
            setManaged(false);
        }
    }
}
