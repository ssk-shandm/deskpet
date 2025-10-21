// PetWindow.java

package com.github.ssk_shandm.deskpet.client.view;

import com.github.ssk_shandm.deskpet.client.network.ApiClient;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PetWindow extends JWindow {

    private final double scale = 0.5;
    private final Map<String, List<BufferedImage>> animations = new HashMap<>();
    private final JLabel imageLabel = new JLabel();
    private Timer animationTimer;
    private String currentAnimationName;
    private int currentFrameIndex;

    private Point mousePressStart;
    private JPopupMenu Menu;
    private JMenuItem exitMenu;
    private JMenuItem cheatlikeabilityItem;
    private JMenuItem hammerItem;
    private JMenuItem pistonItem;
    // 记录上次点击时间戳
    // private long lastClickTime = 0;

    private final ApiClient apiClient = new ApiClient();
    private String petName;
    private int currentLikeability;
    private String currentStatus;
    private Timer dataSyncTimer;

    // 锤子
    private boolean isHammerMode = false;
    private final Cursor hammerCursor = createHammerCursor();
    private final Cursor defaultCursor = Cursor.getDefaultCursor();

    // 推动
    private boolean isPistonMode = false;
    private final Cursor PistonCursor = createPistonCursor();

    /**
     * 入口
     */
    public PetWindow(String petName, int initialLikeability, String initialStatus) {
        this.petName = petName;
        this.currentLikeability = initialLikeability;
        this.currentStatus = initialStatus;

        // 创建并显示一个空窗口
        setupWindow();
        setVisible(true);

        // 启动一个后台加载器来处理所有耗时操作
        new AnimationLoader().execute();

        // 设置交互
        setupInteraction();
    }

    /**
     * 初始化窗口的基本属性
     */
    private void setupWindow() {
        setAlwaysOnTop(true);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));
        add(imageLabel);
        pack();
    }

    /**
     * 实现后台加载
     */
    private class AnimationLoader extends SwingWorker<Void, BufferedImage> {

        /**
         * 动画加载逻辑
         */
        @Override
        protected Void doInBackground() throws Exception {
            // 加载优先动画
            System.out.println("后台：开始加载优先动画...");
            Map<String, List<BufferedImage>> priorityAnimations = loadPriorityAnimations(petName, currentLikeability);
            animations.putAll(priorityAnimations);

            // 第一帧
            String initialAnimationName = getDefaultIdleAnimation(currentLikeability);
            if (!animations.containsKey(initialAnimationName) || animations.get(initialAnimationName).isEmpty()) {
                initialAnimationName = "idle_normal"; // 回退方案
            }

            if (animations.containsKey(initialAnimationName) && !animations.get(initialAnimationName).isEmpty()) {
                BufferedImage firstFrame = animations.get(initialAnimationName).get(0);
                // 显示
                publish(firstFrame);
                currentAnimationName = initialAnimationName; // 设置当前动画名称
            } else {

                throw new RuntimeException("关键动画 (idle) 加载失败，无法启动宠物");
            }

            // 加载剩余的动画
            System.out.println("后台：开始加载剩余动画...");
            List<String> allTypes = listAnimationTypes(petName);
            List<String> remainingNames = allTypes.stream()
                    .filter(name -> !animations.containsKey(name))
                    .collect(Collectors.toList());

            System.out.println("后台加载列表: " + remainingNames);
            Map<String, List<BufferedImage>> remainingAnimations = loadSpecificAnimations(petName, remainingNames);
            animations.putAll(remainingAnimations);

            return null;
        }

        /**
         * 显示与后程辅助
         */
        @Override
        protected void process(List<BufferedImage> chunks) {
            BufferedImage firstFrame = chunks.get(0);
            if (firstFrame != null) {
                System.out.println("UI线程：收到第一帧，正在显示宠物...");
                // 窗口大小调整和显示
                imageLabel.setIcon(new ImageIcon(firstFrame.getScaledInstance(
                        (int) (firstFrame.getWidth() * scale),
                        (int) (firstFrame.getHeight() * scale),
                        Image.SCALE_SMOOTH)));
                pack();

                startAnimationTimer();
                startDataSyncTimer();
            }
        }

        /**
         * 错误提示
         */
        @Override
        protected void done() {
            try {
                get();
                System.out.println("后台：所有动画已成功加载！总动画数: " + animations.size());
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("后台加载过程中发生严重错误: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(PetWindow.this, "加载宠物资源时发生错误:\n" + e.getCause().getMessage(), "错误",
                        JOptionPane.ERROR_MESSAGE);
                dispose();
            }
        }
    }

    /**
     * 根据好感度获取对应的idle动画名称
     */
    private String getDefaultIdleAnimation(int favorability) {
        if (favorability > 85)
            return "idle_happy";
        if (favorability > 60 || favorability < 84)
            return "idle_normal";
        if (favorability > 30 || favorability < 59)
            return "idle_ignore";
        if (favorability > 0 || favorability < 29)
            return "idle_sad";
        return "idle_normal";
    }

    /**
     * 加载优先动画
     */
    private Map<String, List<BufferedImage>> loadPriorityAnimations(String petName, int favorability) {
        List<String> priorityNames = new ArrayList<>();
        priorityNames.add(getDefaultIdleAnimation(favorability));
        priorityNames.add("happy");
        priorityNames.add("pickup");
        if (!priorityNames.contains("idle_normal")) {
            priorityNames.add("idle_normal");
        }
        System.out.println("优先加载列表: " + priorityNames);
        return loadSpecificAnimations(petName, priorityNames);
    }

    /**
     * 根据动画名称列表加载动画文件
     */
    private Map<String, List<BufferedImage>> loadSpecificAnimations(String petName, List<String> animationNames) {
        Map<String, List<BufferedImage>> loadedAnimations = new HashMap<>();
        for (String animName : animationNames) {
            List<BufferedImage> frames = new ArrayList<>();
            String animationPathPrefix = "/BA/" + petName + "/" + animName + "/";
            int frameIndex = 0;
            while (true) {
                String frameFileName = String.format("%04d.png", frameIndex);
                URL resourceUrl = getClass().getResource(animationPathPrefix + frameFileName);
                if (resourceUrl == null)
                    break;
                try {
                    BufferedImage frame = ImageIO.read(resourceUrl);
                    if (frame != null)
                        frames.add(frame);
                    else
                        break;
                } catch (IOException e) {
                    System.err.println("加载动画帧 " + animationPathPrefix + frameFileName + " 时出错: " + e.getMessage());
                    break;
                }
                frameIndex++;
            }
            if (!frames.isEmpty()) {
                loadedAnimations.put(animName, frames);
                System.out.println("加载动画: " + animName + " (" + frames.size() + " 帧)");
            } else {
                System.out.println("未找到或加载失败动画: " + animName);
            }
        }
        return loadedAnimations;
    }

    /**
     * 从资源目录获取所有动画类型的列表
     */
    private List<String> listAnimationTypes(String petName) {
        // 实际部署时，更稳妥的方式是从一个配置文件读取动画列表，而不是扫描目录
        // 这里为了简单，继续使用扫描目录或硬编码列表的方式
        String basePath = "client/resources/BA/" + petName;
        File baseDir = new File(basePath);
        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] subDirs = baseDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                return Arrays.stream(subDirs).map(File::getName).collect(Collectors.toList());
            }
        }
        System.err.println("警告：找不到宠物资源目录: " + basePath + "，将使用硬编码的回退列表");
        return Arrays.asList("idle_normal", "happy", "pickup", "attack", "jump", "idle_happy", "idle_sad",
                "idle_ignore", "headache", "knockdown", "skill");
    }

    /**
     * 启动或恢复动画播放计时器
     */
    private void startAnimationTimer() {
        if (animationTimer != null && animationTimer.isRunning())
            return;
        animationTimer = new Timer(1000 / 30, e -> {
            if (currentAnimationName == null || !animations.containsKey(currentAnimationName))
                return;
            List<BufferedImage> currentFrames = animations.get(currentAnimationName);
            if (currentFrames == null || currentFrames.isEmpty())
                return;

            currentFrameIndex = (currentFrameIndex + 1) % currentFrames.size();
            BufferedImage frameToShow = currentFrames.get(currentFrameIndex);
            imageLabel.setIcon(new ImageIcon(frameToShow.getScaledInstance(
                    (int) (frameToShow.getWidth() * scale),
                    (int) (frameToShow.getHeight() * scale),
                    Image.SCALE_SMOOTH)));

            if (currentFrameIndex == currentFrames.size() - 1 && !isLoopingAnimation(currentAnimationName)) {
                playAnimation(getDefaultIdleAnimation(currentLikeability));
            }
        });
        animationTimer.start();
    }

    /**
     * 播放指定名称的动画
     */
    public void playAnimation(String animationName) {
        if (animationName == null || animationName.equals(currentAnimationName))
            return;
        if (!animations.containsKey(animationName) || animations.get(animationName).isEmpty()) {
            System.out.println("动画 " + animationName + " 尚未加载或为空，无法播放");
            if (currentAnimationName == null || !currentAnimationName.startsWith("idle_")) {
                playAnimation(getDefaultIdleAnimation(currentLikeability));
            }
            return;
        }
        System.out.println("播放动画: " + animationName);
        currentAnimationName = animationName;
        currentFrameIndex = -1;
        if (animationTimer != null && !animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    /**
     * 判断动画是否需要循环播放
     */
    private boolean isLoopingAnimation(String animationName) {
        return animationName != null && animationName.startsWith("idle_");
    }

    /**
     * 启动周期性数据同步计时器
     */
    private void startDataSyncTimer() {
        if (dataSyncTimer != null && dataSyncTimer.isRunning())
            return;
        dataSyncTimer = new Timer(5000, e -> syncData());
        dataSyncTimer.start();
    }

    /**
     * 从服务器异步获取并更新宠物数据
     */
    private void syncData() {
        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() throws Exception {
                return apiClient.getPetData();
            }

            @Override
            protected void done() {
                try {
                    String[] fetchedData = get();
                    if (fetchedData == null || fetchedData.length != 3) {
                        System.err.println("同步失败：获取的数据格式无效");
                        return;
                    }
                    int fetchedLikeability = Integer.parseInt(fetchedData[1]);
                    String fetchedStatus = fetchedData[2];

                    if (fetchedLikeability != currentLikeability) {
                        String oldIdle = getDefaultIdleAnimation(currentLikeability);
                        currentLikeability = fetchedLikeability;
                        System.out.println("好感度更新为: " + currentLikeability);
                        String newIdle = getDefaultIdleAnimation(currentLikeability);
                        if (!oldIdle.equals(newIdle) && currentAnimationName.equals(oldIdle)) {
                            playAnimation(newIdle);
                        }
                    }
                    if (!fetchedStatus.equals(currentStatus)) {
                        currentStatus = fetchedStatus;
                        System.out.println("状态更新为: " + currentStatus);
                    }
                } catch (Exception e) {
                    System.err.println("同步数据时出错: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * 初始化所有鼠标交互
     */
    private void setupInteraction() {
        MyMouseAdapter mouseAdapter = new MyMouseAdapter();
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        RightMenu();
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

        // 锤子
        hammerItem = new JMenuItem("敲击!");
        hammerItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        hammerItem.addActionListener(e -> toggleHammerMode());
        Menu.add(hammerItem);

        // 轻推
        pistonItem = new JMenuItem("我推!");
        pistonItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        pistonItem.addActionListener(e -> togglePistonMode());
        Menu.add(
                pistonItem);

        // 退出
        exitMenu = new JMenuItem("退出");
        exitMenu.addActionListener(e -> {
            System.out.println("退出...");
            System.exit(0);
        });

        Menu.add(exitMenu);

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
        hammerItem.addMouseListener(childMouseListener);
        exitMenu.addMouseListener(childMouseListener);
    }

    /**
     * 创建锤子光标
     */
    private Cursor createHammerCursor() {
        try {
            URL imageUrl = getClass().getResource("/tools/hammer.png");

            Image cursorImage = ImageIO.read(imageUrl);
            Point hotspot = new Point(8, 8);
            return Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, hotspot, "hammer");
        } catch (IOException e) {
            System.err.println("加载 'hammer.png' 光标失败，使用备用光标: " + e.getMessage());
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
    }

    /**
     * 切换锤子模式
     */
    private void toggleHammerMode() {
        isHammerMode = !isHammerMode;

        if (isHammerMode) {
            setCursor(hammerCursor);
            hammerItem.setText("取消锤子");
        } else {
            setCursor(defaultCursor);
            hammerItem.setText("锤子");
        }
    }

    /**
     * 创建活塞
     */
    private Cursor createPistonCursor() {
        try {
            URL imageUrl = getClass().getResource("/tools/piston.gif");

            Image cursorImage = ImageIO.read(imageUrl);
            Point hotspot = new Point(8, 8);
            return Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, hotspot, "piston");
        } catch (IOException e) {
            System.err.println("加载 'pison.png' 光标失败，使用备用光标: " + e.getMessage());
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
    }

    /**
     * 切换活塞模式
     */
    private void togglePistonMode() {
        isPistonMode = !isPistonMode;

        if (isPistonMode) {
            setCursor(PistonCursor);
            pistonItem.setText("取消推动");
        } else {
            setCursor(defaultCursor);
            pistonItem.setText("我推!");
        }
    }

    /**
     * 处理所有与鼠标相关的交互逻辑
     */
    private class MyMouseAdapter extends MouseAdapter {

        /**
         * 处理右键点击
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                Menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        /**
         * 处理左键点击
         */
        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {

                if (currentAnimationName != null && !isLoopingAnimation(currentAnimationName)) {

                    if ("pickup".equals(currentAnimationName)) {
                        mousePressStart = e.getPoint();
                    }

                    return;
                }
                if (isHammerMode) {
                    System.out.println("锤击！");
                    playAnimation("headache");

                    int decreaseAmount = -5;
                    new Thread(() -> {
                        System.out.println("好感度变化: " + decreaseAmount);
                        apiClient.updateLikeability(decreaseAmount);
                    }).start();

                    toggleHammerMode();

                } else if (isPistonMode) {
                    System.out.println("我推！");
                    playAnimation("knockdown");

                    int decreaseAmount = -10;
                    new Thread(() -> {
                        System.out.println("好感度变化: " + decreaseAmount);
                        apiClient.updateLikeability(decreaseAmount);
                    }).start();

                    togglePistonMode();
                } else {
                    mousePressStart = e.getPoint();

                    if (isClickOnCooldown()) {
                        playAnimation("pickup");
                    } else {
                        playAnimation("happy");
                        lastClickTime = System.currentTimeMillis();
                    }
                }
            }
        }

        /**
         * 处理左键释放
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                mousePressStart = null;
                if ("pickup".equals(currentAnimationName)) {
                    playAnimation(getDefaultIdleAnimation(currentLikeability));
                }
            }
        }

        /**
         * 处理鼠标拖拽
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && mousePressStart != null
                    && "pickup".equals(currentAnimationName)) {
                Point newLocation = new Point(
                        getLocation().x + e.getX() - mousePressStart.x,
                        getLocation().y + e.getY() - mousePressStart.y);
                setLocation(newLocation);
            }
        }
    }
}