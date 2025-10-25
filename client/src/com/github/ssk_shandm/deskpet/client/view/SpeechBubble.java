package com.github.ssk_shandm.deskpet.client.view;

import javax.swing.*;
import java.awt.*;

public class SpeechBubble extends JWindow {

    private final Window parentWindow; // 父窗口 (PetWindow)
    private final JLabel textLabel;
    private Timer hideTimer;

    // (新增) 定义气泡的样式
    private final Color backgroundColor = new Color(255, 255, 220); // 气泡背景色
    private final Color borderColor = Color.BLACK; // 气泡边框色
    private final int arcSize = 20; // 圆角的大小 (像素)

    public SpeechBubble(Window parent, GraphicsConfiguration gc) {

        // (关键) 使用带有 gc 的 JWindow 构造函数
        super(parent, gc);

        getRootPane().setOpaque(false);

        this.parentWindow = parent;

        // --- 窗口基础设置 ---
        setBackground(new Color(0, 0, 0, 0)); // 1. 窗口背景必须透明
        setLayout(new BorderLayout());
        setAlwaysOnTop(true);

        // --- (修改) 使用自定义的 BubblePanel ---
        BubblePanel bubblePanel = new BubblePanel();
        bubblePanel.setLayout(new BorderLayout());
        // (注意: 我们不再设置 bubblePanel 的 setBackground 或 setBorder)

        textLabel = new JLabel();
        textLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        textLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); // 内边距

        // (新增) 标签也必须透明, 否则它会画一个矩形背景
        textLabel.setOpaque(false);
        // (可选) 设置前景色
        textLabel.setForeground(Color.BLACK);

        bubblePanel.add(textLabel, BorderLayout.CENTER);

        setContentPane(bubblePanel);

        // 初始化计时器 (一次性触发)
        hideTimer = new Timer(3000, e -> setVisible(false));
        hideTimer.setRepeats(false); // 只触发一次
    }

    /**
     * 显示气泡 (方法签名来自上次修改)
     * 
     * @param text       要显示的文字
     * @param durationMs 显示时长
     * @param anchorY    (新增) 锚点 (宠物头顶) 的屏幕 Y 坐标
     */
    public void showBubble(String text, int durationMs, int anchorY) {
        textLabel.setText(text);

        // (TODO: 下一步根据 bubbleScale 调整字体和大小)
        // Font scaledFont = textLabel.getFont().deriveFont((float)(14 * bubbleScale));
        // textLabel.setFont(scaledFont);

        pack(); // public class SpeechBubble extends JWindow

        // 计算位置 (显示在宠物上方)
        Point parentLocation = parentWindow.getLocation();

        // X 坐标 = 居中
        int x = parentLocation.x + (parentWindow.getWidth() / 2) - (getWidth() / 2);

        // Y 坐标 = 锚点 Y - 气泡高度 - 10
        int y = anchorY - getHeight() - 10; // 在锚点上方 10 像素

        setLocation(x, y);

        // 重启计时器
        hideTimer.setDelay(durationMs);
        hideTimer.restart();

        setVisible(true);
    }

    // --- (新增) 内部类: 自定义绘制面板 ---

    /**
     * 这是一个内部类, 专门负责绘制圆角矩形的背景
     */
    private class BubblePanel extends JPanel {

        public BubblePanel() {
            // (关键) 必须设置为 false,
            // 否则 JPanel 会自己画一个不透明的矩形背景
            setOpaque(false);
        }

        /**
         * (关键) 重写此方法来自定义组件的外观
         */
        @Override
        protected void paintComponent(Graphics g) {
            // (重要) 必须先调用 super.paintComponent(g);
            // 除非你完全接管绘制 (比如这里我们就不需要它)
            // super.paintComponent(g);

            // 我们需要 Graphics2D 来实现更高级的绘制 (例如抗锯齿)
            Graphics2D g2d = (Graphics2D) g.create(); // 复制 g, 避免污染

            // (重要) 开启抗锯齿, 让圆角边缘平滑
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制气泡背景
            g2d.setColor(backgroundColor);
            // fillRoundRect 绘制一个填充的圆角矩形
            g2d.fillRoundRect(
                    0, 0, // X, Y (左上角)
                    getWidth() - 1, // 宽度
                    getHeight() - 1, // 高度
                    arcSize, arcSize // 圆角的水平和垂直直径
            );

            // 绘制气泡边框
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(1)); // 设置边框粗细为 1 像素
            // drawRoundRect 绘制一个描边的圆角矩形
            g2d.drawRoundRect(
                    0, 0,
                    getWidth() - 1,
                    getHeight() - 1,
                    arcSize, arcSize);

            // 释放 Graphics2D 资源
            g2d.dispose();

            // (注意) 我们重写了这个方法后, Swing 会自动为我们绘制子组件
            // (即 JLabel), 所以我们不需要在这里手动绘制文字。
        }
    }
}