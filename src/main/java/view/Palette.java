package view;

import javafx.scene.paint.Color;
import model.BeanType;

/**
 * 主题调色板(A 模块内部):按主题返回配色表(棋盘格纹/障碍/蛇头尾渐变/五类豆色值);
 * 换主题只换色值,绘制代码一份;五类豆为跨主题语义色,两套主题共用
 */
public final class Palette {

    // ===== 主题键 =====

    /**
     * 主题键:id/展示名与 MapCatalog、存档引用约定一致;未知 id 快速失败,
     * 保证新增地图布局时不会漏配主题色
     */
    public enum Theme {

        /** 管道主题 */
        PIPE("pipe", "管道"),

        /** 雪山主题 */
        SNOW("snow", "雪山");

        /** 地图标识(与 GameMap.id 一致) */
        public final String id;

        /** 展示名(与 GameMap.displayName 一致) */
        public final String displayName;

        Theme(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /** 按地图标识匹配主题,取不到抛 IllegalArgumentException */
        public static Theme of(String mapId) {
            for (Theme theme : values()) {
                if (theme.id.equalsIgnoreCase(mapId)) {
                    return theme;
                }
            }
            throw new IllegalArgumentException("未注册主题: " + mapId);
        }
    }

    // ===== 主题取色入口 =====

    /** 按主题取调色板 */
    public static Palette of(Theme theme) {
        return switch (theme) {
            case PIPE -> PIPE_PALETTE;
            case SNOW -> SNOW_PALETTE;
        };
    }

    /** 按地图标识取调色板(渲染入口;未注册主题快速失败) */
    public static Palette ofMapId(String mapId) {
        return of(Theme.of(mapId));
    }

    // ===== 两套主题实例 =====

    /** 管道:暖米格纹 + 海绿水管障碍 + 亮蓝蛇(与绿管/暖豆均不撞色) */
    private static final Palette PIPE_PALETTE = new Palette(
            "#F6EFE0", // 棋盘浅格
            "#E7D9BC", // 棋盘深格
            "#2E8B57", // 障碍:管道海绿
            "#3498DB", // 蛇头(渐变亮端)
            "#1B4F72"  // 蛇尾(渐变暗端)
    );

    /** 雪山:冰白格纹 + 冰峰蓝障碍 + 青绿蛇(冷底上唯一跳出的活物色) */
    private static final Palette SNOW_PALETTE = new Palette(
            "#EBF5FB", // 棋盘浅格
            "#D6EAF8", // 棋盘深格
            "#2E86C1", // 障碍:冰峰蓝
            "#1ABC9C", // 蛇头(渐变亮端)
            "#0E6251"  // 蛇尾(渐变暗端)
    );

    // ===== 实例数据 =====

    /** 棋盘格纹浅色 */
    private final Color boardLight;

    /** 棋盘格纹深色 */
    private final Color boardDark;

    /** 障碍填充色 */
    private final Color obstacle;

    /** 蛇头色(渐变亮端;中间节由绘制方 Color.interpolate(head, tail, t) 插值) */
    private final Color snakeHead;

    /** 蛇尾色(渐变暗端) */
    private final Color snakeTail;

    /** 私有构造:仅主题静态实例可建 */
    private Palette(String boardLightHex, String boardDarkHex, String obstacleHex,
                    String snakeHeadHex, String snakeTailHex) {
        this.boardLight = Color.web(boardLightHex);
        this.boardDark = Color.web(boardDarkHex);
        this.obstacle = Color.web(obstacleHex);
        this.snakeHead = Color.web(snakeHeadHex);
        this.snakeTail = Color.web(snakeTailHex);
    }

    /** 棋盘格纹浅色 */
    public Color boardLight() {
        return boardLight;
    }

    /** 棋盘格纹深色 */
    public Color boardDark() {
        return boardDark;
    }

    /** 障碍填充色 */
    public Color obstacle() {
        return obstacle;
    }

    /** 蛇头色(渐变亮端) */
    public Color snakeHead() {
        return snakeHead;
    }

    /** 蛇尾色(渐变暗端) */
    public Color snakeTail() {
        return snakeTail;
    }

    // ===== 五类豆语义色(跨主题统一:红=小/橙=大/金=减负/紫=毒/深紫=大毒) =====

    /** 小豆色 */
    private static final Color SMALL_COLOR = Color.web("#E74C3C");

    /** 大豆色 */
    private static final Color BIG_COLOR = Color.web("#E67E22");

    /** 减负金豆色(比纯金略深,浅色棋盘上不飘) */
    private static final Color GOLD_COLOR = Color.web("#E6A700");

    /** 毒豆色 */
    private static final Color POISON_COLOR = Color.web("#9B59B6");

    /** 大毒豆色 */
    private static final Color BIG_POISON_COLOR = Color.web("#6C3483");

    /** 按豆类别取色(小/大/金/毒/大毒) */
    public Color beanColor(BeanType type) {
        return switch (type) {
            case SMALL -> SMALL_COLOR;
            case BIG -> BIG_COLOR;
            case GOLD -> GOLD_COLOR;
            case POISON -> POISON_COLOR;
            case BIG_POISON -> BIG_POISON_COLOR;
        };
    }
}
