package com.github.ssk_shandm.deskpet.client.view;

import javax.swing.*;
import java.awt.*;

/**
 * 语音气泡窗口 (JWindow)
 * 负责显示一个带圆角的、自定义背景的文本气泡。
 */
public class SpeechBubble extends JWindow {

    private final Window parentWindow; // 父窗口 (PetWindow)
    private final JLabel textLabel; // 显示文本的标签
    private static final java.util.logging.Logger logger = java.util.logging.Logger
            .getLogger(SpeechBubble.class.getName());
    private Timer hideTimer; // 自动隐藏计时器

    // 气泡样式常量
    private final Color backgroundColor = new Color(255, 255, 220); // 背景色
    private final Color borderColor = Color.BLACK; // 边框色
    private final int arcSize = 20; // 圆角大小 (px)

    /**
     * 构造函数
     * * @param parent 父窗口 (PetWindow)
     * @param gc     图形配置 (用于透明窗口)
     */
    public SpeechBubble(Window parent, GraphicsConfiguration gc) {
        // 使用带 gc 的构造函数以支持透明
        super(parent, gc);
        this.parentWindow = parent;

        // 窗口基础设置
        getRootPane().setOpaque(false); // 根面板透明
        setBackground(new Color(0, 0, 0, 0)); // 窗口背景透明
        setLayout(new BorderLayout());
        setAlwaysOnTop(true);

        // 使用自定义的 BubblePanel 作为内容面板
        BubblePanel bubblePanel = new BubblePanel();
        bubblePanel.setLayout(new BorderLayout());

        // 配置 JLabel
        textLabel = new JLabel();
        textLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        textLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); // 内边距
        textLabel.setOpaque(false); // 标签本身也必须透明
        textLabel.setForeground(Color.BLACK);

        // 组装
        bubblePanel.add(textLabel, BorderLayout.CENTER);
        setContentPane(bubblePanel);

        // 初始化计时器
        hideTimer = new Timer(3000, e -> setVisible(false));
        hideTimer.setRepeats(false); // 一次性触发
    }

    /**
     * 显示气泡
     * * @param text       要显示的文字
     * @param durationMs 显示时长 (毫秒)
     * @param anchorY    锚点 (宠物头顶) 的屏幕 Y 坐标
     */
    public void showBubble(String text, int durationMs, int anchorY) {
        textLabel.setText(text);

        // 打包以获取最佳大小
        pack();

        // 计算位置
        Point parentLocation = parentWindow.getLocation();

        // X 坐标: 在父窗口上居中
        int x = parentLocation.x + (parentWindow.getWidth() / 2) - (getWidth() / 2);

        // Y 坐标: 在锚点 Y 之上 (留出 10px 间隙)
        int y = anchorY - getHeight() - 10;

        setLocation(x, y);

        logger.info("SpeechBubble 正在设置时长为: " + durationMs + "ms");

        // 重启计时器 (设置新的延迟并启动)
        hideTimer.setInitialDelay(durationMs);
        hideTimer.setDelay(durationMs);
        hideTimer.restart();

        // 显示
        setVisible(true);
    }

    /**
     * 内部类：自定义面板
     * 负责绘制圆角矩形背景和边框。
     */
    private class BubblePanel extends JPanel {

        public BubblePanel() {
            // 必须设置为 false, 否则 JPanel 会绘制不透明的矩形背景
            setOpaque(false);
        }

        /**
         * 重写 paintComponent 以自定义绘制
         */
        @Override
        protected void paintComponent(Graphics g) {
            // 不调用 super.paintComponent(g), 因为我们完全自定义背景

            Graphics2D g2d = (Graphics2D) g.create(); // 使用副本

            // 开启抗锯齿, 使圆角平滑
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制背景 (填充圆角矩形)
            g2d.setColor(backgroundColor);
            g2d.fillRoundRect(
                    0, 0, // X, Y
                    getWidth() - 1, // 宽度
                    getHeight() - 1, // 高度
                    arcSize, arcSize // 圆角
            );

            // 绘制边框 (描边圆角矩形)
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(1)); // 1 像素边框
            g2d.drawRoundRect(
                    0, 0,
                    getWidth() - 1,
                    getHeight() - 1,
                    arcSize, arcSize);

            // 释放资源
            g2d.dispose();

            // Swing 会在此之后自动绘制子组件 (JLabel)
        }
    }
}