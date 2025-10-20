package com.github.ssk_shandm.deskpet.client.view;

import com.github.ssk_shandm.deskpet.client.network.ApiClient;

import javax.swing.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.Random;

public class PetWindow extends JWindow {

    // 缩放比例
    private final double scale = 0.7;

    // 动画仓库
    private final Map<String, List<BufferedImage>> animations = new HashMap<>();
    private final JLabel imageLabel = new JLabel();
    private Timer animationTimer;
    private String currentAnimationName;
    private int currentFrameIndex;

    // 交互变量
    private Point mousePressStart;

    // 右键菜单
    private JPopupMenu Menu;
    private JMenuItem exitMenu;
    // private JMenuItem likeabilityItem;
    // private JMenuItem statusItem;
    // private JMenuItem nameItem;
    // private JMenuItem rankItem;
    private JMenuItem cheatlikeabilityItem;

    // 客户端核心逻辑
    private final ApiClient apiClient = new ApiClient();
    private String petName = "";
    private int currentLikeability = -1;
    private String currentStatus = "";
    private Timer dataSyncTimer; // 数据同步定时器

    // 点击冷却
    private final long ONE_HOUR_IN_MS = 60 * 60 * 1000;
    private long lastSuccessfulClickTime = 0;

    // 随机移动变量
    // private Timer movementTimer; // 决定器
    private boolean isMoving = false; // 是否移动状态锁
    private Point targetPosition; // 移动的目标位置
    private double currentVelocityX; // X轴
    private double currentVelocityY; // Y轴
    private int animationLoopCount = 0; // 循环次数

    public PetWindow() {
        loadAnimations();

        // setSize(718, 926);

        if (animations.isEmpty()) {
            System.err.println("错误：没有任何动画被成功加载！程序将退出。");
            return;
        }

        // 窗口属性
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));
        add(imageLabel);

        // 窗口位置
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - 300) / 2, (screenSize.height - 300) / 2);

        // 添加鼠标事件监听器 - 点击
        addMouseListener(new java.awt.event.MouseAdapter() {
            /**
             * 检查冷却状态,动作决策
             */
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {

                // 右键响应模式
                if (e.isPopupTrigger()) {
                    showMenu(e);
                    return;
                }

                // 只响应鼠标左键
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                // 防干扰逻辑
                if ("happy".equals(currentAnimationName)) {
                    System.out.println("正在播放 happy 动画");
                    return;
                }

                if (isPixelTransparent(e.getX(), e.getY())) {
                    return;
                }

                long currentTime = System.currentTimeMillis();
                boolean isInCooldown = (currentTime - lastSuccessfulClickTime < ONE_HOUR_IN_MS);

                if (isInCooldown) {
                    System.out.println("冷却中，进入拖动模式...");

                    if (isMoving) {
                        isMoving = false;
                        System.out.println("移动被用户交互中断。");
                    }

                    mousePressStart = e.getPoint();
                    playAnimation("pickup");
                } else {
                    System.out.println("有效点击！增加好感度。");

                    // 更新冷却时间
                    lastSuccessfulClickTime = currentTime;

                    playAnimation("happy");

                    new Thread(() -> {
                        apiClient.updateLikeability(5);
                    }).start();
                }
            }

            /**
             * 鼠标松开的瞬间恢复待机状态, 并重置拖动点
             */
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {

                if (e.isPopupTrigger()) {
                    showMenu(e);
                    return;
                }

                if ("pickup".equals(currentAnimationName))
                    playIdleWithLikeability();
                mousePressStart = null;

            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
            }

            /**
             * 鼠标进入窗口时隐藏菜单
             */
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (Menu != null && Menu.isVisible()) {
                    Menu.setVisible(false);
                }
            }

            /**
             * 右键菜单像素点检测
             */
            private void showMenu(java.awt.event.MouseEvent e) {
                if (!isPixelTransparent(e.getX(), e.getY())) {
                    Menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        RightMenu();

        // 添加鼠标事件监听器 - 拖拽
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (mousePressStart == null)
                    return;
                int newX = e.getXOnScreen() - mousePressStart.x;
                int newY = e.getYOnScreen() - mousePressStart.y;
                setLocation(newX, newY);
            }
        });

        // 数据同步计时器
        dataSyncTimer = new Timer(5000, evt -> syncDataWithServer());
        dataSyncTimer.setInitialDelay(0); // 立即执行第一次
        dataSyncTimer.start();

        // 随机移动计时器
        // movementTimer = new Timer(10000, e -> decideToMove());
        // movementTimer.start();

    }

    /**
     * 右键菜单
     */
    private void RightMenu() {
        Menu = new JPopupMenu();

        // 信息显示
        final JMenuItem infoItem = new JMenuItem();
        infoItem.setEnabled(false);
        infoItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        Menu.add(infoItem);
        Menu.addSeparator();

        Menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                String infoText = String.format("名称: %s | 状态: '%s' | 好感度: %d",
                        petName,
                        currentStatus,
                        currentLikeability);
                infoItem.setText(infoText);
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });

        // 好感度修改
        cheatlikeabilityItem = new JMenuItem("likeability test:");

        cheatlikeabilityItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        cheatlikeabilityItem.setBackground(Menu.getBackground());
        cheatlikeabilityItem.setForeground(Color.ORANGE);

        cheatlikeabilityItem.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(PetWindow.this, "(0-100):", currentLikeability);

            if (input == null) {
                return;
            }

            try {
                int newLikeability = Integer.parseInt(input);

                if (newLikeability > 100)
                    newLikeability = 100;
                if (newLikeability < 0)
                    newLikeability = 0;

                int difference = newLikeability - currentLikeability;

                if (difference == 0) {
                    return;
                }

                new Thread(() -> {
                    System.out.println("change:" + difference);
                    apiClient.updateLikeability(difference);
                }).start();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(PetWindow.this, "none", "error", JOptionPane.ERROR_MESSAGE);
            }
        });
        Menu.add(cheatlikeabilityItem);

        // 退出
        exitMenu = new JMenuItem("退出");
        exitMenu.addActionListener(e -> {
            System.out.println("退出...");
            System.exit(0);
        });

        Menu.add(exitMenu);

        // 右键菜单消耗逻辑
        Menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (!Menu.contains(e.getPoint())) {
                    Menu.setVisible(false);
                }
            }
        });
        MouseAdapter childMouseListener = new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), Menu);
                if (!Menu.contains(p)) {
                    Menu.setVisible(false);
                }
            }
        };
        infoItem.addMouseListener(childMouseListener);
        cheatlikeabilityItem.addMouseListener(childMouseListener);
        exitMenu.addMouseListener(childMouseListener);
    }

    /**
     * 数据同步,定期获取服务器数据并更新UIdecideToMove
     */
    private void syncDataWithServer() {
        new Thread(() -> {
            String[] data = apiClient.getPetData();
            if (data != null && data.length == 3) {
                String nameFromServer = data[0];
                int likeabilityFromServer = Integer.parseInt(data[1]); // 好感度
                String statusFromServer = data[2]; // 状态

                // 数据同步逻辑
                if (likeabilityFromServer != currentLikeability || !statusFromServer.equals(currentStatus)) {
                    System.out.println("数据同步：好感度 " + currentLikeability + " -> " + likeabilityFromServer +
                            ", 状态 '" + currentStatus + "' -> '" + statusFromServer + "'");

                    petName = nameFromServer;
                    currentLikeability = likeabilityFromServer;
                    currentStatus = statusFromServer;

                    // 分线程通讯主线程
                    SwingUtilities.invokeLater(() -> {
                        // 防干扰逻辑
                        if ("fainted".equals(currentStatus)) {
                            playAnimation("faint");
                        } else {
                            if (!"pickup".equals(currentAnimationName) && !"happy".equals(currentAnimationName)) {
                                playIdleWithLikeability();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 加载所有动画资源
     */
    private void loadAnimations() {
        System.out.println("===== 开始加载动画资源... =====");
        String characterFolderName = "BA/seia";

        // 所有动画
        String[] animationNames = {

                "idle_happy", "idle_normal", "idle_ignore", "idle_sad",
                "pickup", "happy", "faint",
                "walk_n", "walk_s", "walk_e", "walk_w", "walk_ne", "walk_nw", "walk_se", "walk_sw",
                "attack", "jump", "knockdown", "skill"
        };

        // 逐个动画加载
        for (String animationName : animationNames) {
            List<BufferedImage> frames = new ArrayList<>();
            int frameIndex = 0;
            while (true) {
                String fileName = String.format("%04d.png", frameIndex);
                String framePath = characterFolderName + "/" + animationName + "/" + fileName;
                URL frameUrl = getClass().getClassLoader().getResource(framePath);
                if (frameUrl != null) {
                    try {
                        frames.add(ImageIO.read(frameUrl));
                        frameIndex++;
                    } catch (IOException e) {
                        System.err.println("错误：无法读取图片 " + framePath);
                        break;
                    }
                } else {
                    break;
                }
            }
            if (!frames.isEmpty()) {
                System.out.println("--- 总结: 成功加载动画 '" + animationName + "' (" + frames.size() + " 帧) ---");
                animations.put(animationName, frames);
            } else {
                System.out.println("--- 总结: 未能为 '" + animationName + "' 加载任何帧 ---");
            }
        }
        System.out.println("\n===== 所有动画资源加载完毕 =====");
    }

    /**
     * 根据好感度,播放不同的 idle 动画
     */
    private void playIdleWithLikeability() {
        if (currentLikeability > 85) {
            playAnimation("idle_happy");
        } else if (currentLikeability > 60) {
            playAnimation("idle_normal");
        } else if (currentLikeability > 30) {
            playAnimation("idle_ignore");
        } else if (currentLikeability > 0) {
            playAnimation("idle_sad");
        }
        // 如果好感度为0, dataSyncTimer 会自动切换到 faint
    }

    /**
     * 播放指定动画
     * * @param name 动画名称
     */
    public void playAnimation(String name) {
        if (!animations.containsKey(name) || name.equals(currentAnimationName)) {
            return;
        }
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // 重置动画循环计数器
        animationLoopCount = 0;

        // 如果数据更新,则播放新数据对应的动画
        currentAnimationName = name;
        currentFrameIndex = 0; // 重置帧索引
        animationTimer = new Timer(17, e -> { // 动画帧率 (毫秒)
            int frameCount = animations.get(name).size();
            if (frameCount == 0)
                return;

            // 记录更新前的帧, 用于判断循环
            int oldFrameIndex = currentFrameIndex;
            currentFrameIndex = (currentFrameIndex + 1) % frameCount;

            // 如果帧索引变小了, 说明完成了一次循环
            if (currentFrameIndex < oldFrameIndex) {
                animationLoopCount++;
            }

            // 移动动画播放两次后自动停止
            if (isMoving && currentAnimationName.startsWith("walk") && animationLoopCount >= 2) {
                isMoving = false; // 结束移动状态
                playIdleWithLikeability(); // 恢复待机
                return;
            }

            // 一次性动画的处理
            boolean isOneShotAnimation = "happy".equals(currentAnimationName) || "faint".equals(currentAnimationName);
            if (isOneShotAnimation && animationLoopCount >= 1) {
                playIdleWithLikeability();
                return;
            }

            // 移动逻辑
            if (isMoving) {
                Point currentPos = getLocation();

                // 检查是否已接近目标点
                if (currentPos.distance(targetPosition) > 5) {
                    int nextX = (int) (currentPos.x + currentVelocityX);
                    int nextY = (int) (currentPos.y + currentVelocityY);
                    setLocation(nextX, nextY);
                } else {
                    // 已到达目标点
                    isMoving = false; // 结束移动状态
                    setLocation(targetPosition); // 精准定位到目标
                    playIdleWithLikeability(); // 恢复待机
                }
            }

            updatePetImage();
        });
        animationTimer.start();
        updatePetImage();
    }

    /**
     * 根据帧索引更新图片
     */
    private void updatePetImage() {
        List<BufferedImage> frames = animations.get(currentAnimationName);
        if (frames == null || frames.isEmpty())
            return;

        // BufferedImage currentFrame = frames.get(currentFrameIndex);
        // imageLabel.setIcon(new ImageIcon(currentFrame));

        // 调整窗口大小,适应帧图尺寸
        // if (getWidth() != currentFrame.getWidth() || getHeight() !=
        // currentFrame.getHeight()) {
        // setSize(currentFrame.getWidth(), currentFrame.getHeight());
        // }

        BufferedImage originalFrame = frames.get(currentFrameIndex);

        int newWidth = (int) (originalFrame.getWidth() * scale);
        int newHeight = (int) (originalFrame.getHeight() * scale);

        Image scaledImage = originalFrame.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

        imageLabel.setIcon(new ImageIcon(scaledImage));

        if (getWidth() != newWidth || getHeight() != newHeight) {
            setSize(newWidth, newHeight);
        }
    }

    /**
     * 检查像素是否透明
     */
    private boolean isPixelTransparent(int x, int y) {
        List<BufferedImage> frames = animations.get(currentAnimationName);
        if (frames == null || frames.isEmpty())
            return true;

        BufferedImage originalFrame = frames.get(currentFrameIndex);

        // 比例缩放指针探测
        int originalX = (int) (x / scale);
        int originalY = (int) (y / scale);

        if (originalX < 0 || originalX >= originalFrame.getWidth() || originalY < 0
                || originalY >= originalFrame.getHeight()) {
            return true;
        }
        int pixel = originalFrame.getRGB(originalX, originalY);
        return ((pixel >> 24) & 0xff) == 0;
    }

    /**
     * 决定是否要移动，以及移动到哪里
     */
    // private void decideToMove() {

    // // 防干扰逻辑
    // if (currentAnimationName == null || isMoving ||
    // !currentAnimationName.startsWith("idle")) {
    // return;
    // }

    // Random random = new Random();

    // if (random.nextBoolean()) {
    // System.out.println("开始移动。");

    // // 双屏
    // Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    // // 计算可移动的边界（目标点的左上角坐标范围）
    // int randomBoundX = screenSize.width - getWidth();
    // int randomBoundY = screenSize.height - getHeight();

    // // 只有在有移动空间时才进行
    // if (randomBoundX > 0 && randomBoundY > 0) {
    // int targetX = random.nextInt(randomBoundX);
    // int targetY = random.nextInt(randomBoundY);

    // targetPosition = new Point(targetX, targetY);

    // // 开始执行移动
    // // startMoving();
    // } else {
    // System.out.println("没有足够的空间移动。");
    // }
    // } else {
    // System.out.println("保持原位不动");
    // }
    // }

    /**
     * 根据目标点，计算速度并开始播放移动动画
     */
    // private void startMoving() {
    // isMoving = true; // 进入移动状态

    // Point currentPos = getLocation();
    // double distance = currentPos.distance(targetPosition);

    // // 移动速度计算
    // double standardDistance = 150.0;

    // int animationFrames = animations.getOrDefault("walk_e", List.of()).size();
    // if (animationFrames == 0)
    // animationFrames = 60; // 防止除零错误

    // double totalFramesToTarget = (distance / standardDistance) * animationFrames;

    // currentVelocityX = (targetPosition.x - currentPos.x) / totalFramesToTarget;
    // currentVelocityY = (targetPosition.y - currentPos.y) / totalFramesToTarget;

    // // 根据速度方向，决定播放哪个走路动画
    // double angle = Math.toDegrees(Math.atan2(-currentVelocityY,
    // currentVelocityX));

    // if (angle < 0) {
    // angle += 360;
    // }

    // // 8 个方向
    // String directionAnimation;
    // if (angle >= 337.5 || angle < 22.5) {
    // directionAnimation = "walk_e";
    // } else if (angle >= 22.5 && angle < 67.5) {
    // directionAnimation = "walk_ne";
    // } else if (angle >= 67.5 && angle < 112.5) {
    // directionAnimation = "walk_n";
    // } else if (angle >= 112.5 && angle < 157.5) {
    // directionAnimation = "walk_nw";
    // } else if (angle >= 157.5 && angle < 202.5) {
    // directionAnimation = "walk_w";
    // } else if (angle >= 202.5 && angle < 247.5) {
    // directionAnimation = "walk_sw";
    // } else if (angle >= 247.5 && angle < 292.5) {
    // directionAnimation = "walk_s";
    // } else { // 292.5 to 337.5
    // directionAnimation = "walk_se";
    // }

    // playAnimation(directionAnimation);
    // }

    public void showPet() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }
}
