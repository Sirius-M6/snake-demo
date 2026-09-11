package org.example;

import model.Board;
import model.Point;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Board 代码自检弹窗(临时验证工具,验证通过后可整文件删除):
 * 不依赖 JavaFX 运行环境,直接运行本类 main 即可;
 * 结果同时输出控制台、写入 target/board-check-result.txt(UTF-8)并以弹窗展示。
 */
public class BoardTestWindow {

    /** 通过数 */
    private static int passed = 0;

    /** 失败数 */
    private static int failed = 0;

    /** 逐项结果文本 */
    private static final StringBuilder REPORT = new StringBuilder();

    public static void main(String[] args) {
        runAllChecks();
        System.out.println(REPORT);
        saveReport();
        SwingUtilities.invokeLater(BoardTestWindow::showResultDialog);
    }

    /** 运行全部检查项 */
    private static void runAllChecks() {
        passed = 0;
        failed = 0;
        REPORT.setLength(0);
        REPORT.append("===== Board 自检开始 =====\n");

        // 被测棋盘:20 行 × 20 列,障碍 {(2,3), (5,5)}
        Set<Point> obstacles = new HashSet<>();
        obstacles.add(new Point(2, 3));
        obstacles.add(new Point(5, 5));
        Board board = new Board(20, 20, obstacles);

        // --- inside:界内判定 ---
        check("inside: (0,0) 左上角应在界内", board.inside(new Point(0, 0)));
        check("inside: (19,19) 右下角应在界内", board.inside(new Point(19, 19)));
        check("inside: (20,0) 行越界应判 false", !board.inside(new Point(20, 0)));
        check("inside: (0,20) 列越界应判 false", !board.inside(new Point(0, 20)));
        check("inside: (-1,5) 负行应判 false", !board.inside(new Point(-1, 5)));
        check("inside: (5,-1) 负列应判 false", !board.inside(new Point(5, -1)));

        // --- wrap:穿墙环化 ---
        check("wrap: (-1,5) 应环化为 (19,5)", board.wrap(new Point(-1, 5)).equals(new Point(19, 5)));
        check("wrap: (20,5) 应环化为 (0,5)", board.wrap(new Point(20, 5)).equals(new Point(0, 5)));
        check("wrap: (5,-1) 应环化为 (5,19)", board.wrap(new Point(5, -1)).equals(new Point(5, 19)));
        check("wrap: (5,20) 应环化为 (5,0)", board.wrap(new Point(5, 20)).equals(new Point(5, 0)));
        check("wrap: (3,4) 界内坐标应保持不变", board.wrap(new Point(3, 4)).equals(new Point(3, 4)));
        check("wrap: (-1,-1) 双越界应环化为 (19,19)", board.wrap(new Point(-1, -1)).equals(new Point(19, 19)));
        check("wrap: (21,22) 超出一圈应环化为 (1,2)", board.wrap(new Point(21, 22)).equals(new Point(1, 2)));

        // --- obstacleAt:障碍查询 ---
        check("obstacleAt: (2,3) 应为障碍", board.obstacleAt(new Point(2, 3)));
        check("obstacleAt: (3,2) 行列不颠倒,应为空格", !board.obstacleAt(new Point(3, 2)));
        check("obstacleAt: (0,0) 应为空格", !board.obstacleAt(new Point(0, 0)));

        // --- freeCells:空格枚举 ---
        Set<Point> occupied = new HashSet<>();
        occupied.add(new Point(10, 10)); // 蛇头位置
        occupied.add(new Point(10, 9));
        occupied.add(new Point(10, 8));
        Set<Point> free = board.freeCells(occupied);
        check("freeCells: 数量应为 400-2障碍-3占用=395", free.size() == 395);
        check("freeCells: 不含障碍格 (2,3)", !free.contains(new Point(2, 3)));
        check("freeCells: 不含占用格 (10,10)", !free.contains(new Point(10, 10)));
        check("freeCells: 含空格 (0,0)", free.contains(new Point(0, 0)));
        check("freeCells: 含空格 (19,19)", free.contains(new Point(19, 19)));

        // --- countFree:空格计数 ---
        check("countFree: 应与 freeCells 数量一致(395)", board.countFree(occupied) == 395);
        check("countFree: 无占用时应为 398", board.countFree(new HashSet<>()) == 398);

        // 边界场景:5×5 棋盘剩 9 个空格(用于「空格 < 10 不刷 debuff 类豆子」判定)
        Board small = new Board(5, 5, Set.of(new Point(0, 0)));
        Set<Point> busy = new HashSet<>();
        for (int r = 1; r <= 3; r++) {
            for (int c = 0; c < 5; c++) {
                busy.add(new Point(r, c));
            }
        }
        check("countFree: 5x5 棋盘剩 9 空格(空格<10 判定的边界)", small.countFree(busy) == 9);

        // --- 防御性拷贝:构造后外部修改障碍集不影响棋盘 ---
        Set<Point> mutable = new HashSet<>();
        mutable.add(new Point(1, 1));
        Board copySafe = new Board(5, 5, mutable);
        mutable.add(new Point(2, 2));
        check("构造: 外部修改障碍集不影响棋盘(防御性拷贝)", !copySafe.obstacleAt(new Point(2, 2)));

        REPORT.append("===== 完成:通过 ").append(passed).append(" 项,失败 ").append(failed).append(" 项 =====\n");
    }

    /** 记录一项检查结果 */
    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            REPORT.append("[通过] ").append(name).append('\n');
        } else {
            failed++;
            REPORT.append("[失败] ").append(name).append('\n');
        }
    }

    /** 结果写入 target/board-check-result.txt(UTF-8),便于排查 */
    private static void saveReport() {
        try {
            Files.writeString(Path.of("target", "board-check-result.txt"), REPORT.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            // 写入失败不影响弹窗展示
        }
    }

    /** 弹窗展示自检结果,关闭后退出进程 */
    private static void showResultDialog() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
            // 主题设置失败不影响展示
        }

        boolean allPass = failed == 0;
        JLabel title = new JLabel(allPass
                ? "全部通过:共 " + passed + " 项检查"
                : "存在失败:通过 " + passed + " 项,失败 " + failed + " 项", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(allPass ? new Color(0x2E7D32) : new Color(0xC62828));

        JTextArea area = new JTextArea(REPORT.toString(), 26, 52);
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setCaretPosition(0);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);

        JOptionPane.showMessageDialog(null, panel, "Board 代码自检结果",
                allPass ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        System.exit(0);
    }
}
