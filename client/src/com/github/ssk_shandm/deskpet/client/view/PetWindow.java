package com.github.ssk_shandm.deskpet.client.view;

import com.github.ssk_shandm.deskpet.client.network.ApiClient;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DnDConstants;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * 宠物主窗口 (JWindow)
 * 负责显示动画、处理用户交互、管理状态。
 */
public class PetWindow extends JWindow {

    // 静态常量
    private static final Logger logger = Logger.getLogger(PetWindow.class.getName());
    private static final int SPECIAL_ACTION_COOLDOWN_MS = 60000; // 特殊互动冷却时间 (ms)
    private static final String SPECIAL_ACTION_ANIMATION = "attack"; // 特殊互动动画
    private static final String SPECIAL_ACTION_AUDIO = "ch0070_battle_in_1"; // 特殊互动音频
    private static final int SPECIAL_ACTION_LIKEABILITY_GAIN = 10; // 特殊互动好感度
    private final int PET_VISUAL_TOP_OFFSET = 120; // 气泡相对宠物顶部的偏移量

    // 核心组件
    private final double scale = 0.5; // 图片缩放比例
    private final Map<String, List<BufferedImage>> animations = new java.util.concurrent.ConcurrentHashMap<>(); // 动画帧缓存
    private final JLabel imageLabel = new JLabel(); // 显示动画的标签
    private final ApiClient apiClient = new ApiClient(); // API 客户端
    private final AudioManager audioManager; // 音频管理器
    private final SpeechBubble speechBubble; // 气泡窗口

    // 动画与状态
    private Timer animationTimer; // 动画播放计时器
    private String currentAnimationName; // 当前播放的动画名
    private int currentFrameIndex; // 当前动画帧索引
    private String petName = "加载中..."; // 宠物名称
    private int currentLikeability = 100; // 当前好感度
    private long lastClickTimeFromServer = 0; // 服务器记录的上次点击时间

    // 交互模式
    private Point mousePressStart; // 窗口拖动起始点
    private Cursor hammerCursor; // 锤子光标
    private Cursor PistonCursor; // 活塞光标
    private Cursor defaultCursor; // 默认光标
    private volatile boolean isHammerMode = false; // 锤子模式
    private volatile boolean isPistonMode = false; // 活塞模式
    private volatile boolean isSpecialActionOnCooldown = false; // 特殊互动冷却中

    // 计时器
    private Timer autoSpeechTimer; // 自动说话计时器
    private Timer favorabilityTimer; // 好感度自动降低计时器
    private Timer inactivityTimer; // 用户闲置计时器
    private Timer specialActionCooldownTimer; // 特殊互动冷却计时器
    private boolean isIdleDoubt = false; // 是否处于“疑惑”闲置状态

    // 右键菜单
    private JPopupMenu contextMenu;
    private JMenuItem exitMenuItem;
    private JMenuItem cheatLikeabilityItem;
    private JMenuItem hammerMenuItem;
    private JMenuItem pistonMenuItem;
    private JMenuItem MoveFileMenuItem;
    private JMenuItem specialActionMenuItem;
    private JMenu voiceOptionsMenu;
    private JCheckBoxMenuItem muteMenuItem;
    private JMenu volumeSubMenu;
    private JCheckBoxMenuItem showBubbleMenuItem;
    private JMenu languageSubMenu;

    // 语音与气泡状态
    private boolean isMuted = false;
    private float currentVolume = 0.3f; // 0.0f - 1.0f
    private boolean isBubbleDisabled = false; // 是否禁用气泡

    // 语音过滤
    private final java.util.Set<String> eventOnlyKeys = java.util.Set.of(
            "ch0070_tactic_defeat_1",
            "ch0070_tactic_defeat_2",
            "ch0070_battle_in_1",
            "ch0070_exweapon_get",
            "ch0070_growup_4",
            "ch0070_eventmission_login_2");

    /**
     * 构造函数：初始化窗口和组件
     */
    public PetWindow() {
        // 窗口基础设置 (透明, 置顶)
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));
        setLayout(new BorderLayout());
        add(imageLabel, BorderLayout.CENTER);

        // 设置初始加载状态
        imageLabel.setText("加载中...");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        setSize(150, 50);
        setLocationRelativeTo(null); // 居中
        setVisible(true);

        // 初始化核心组件
        createCursors(); // 初始化自定义光标
        this.audioManager = new AudioManager();
        this.speechBubble = new SpeechBubble(this, getGraphicsConfiguration());

        // 初始化计时器
        this.autoSpeechTimer = new Timer(120000, e -> sayRandomly()); // 2分钟
        this.autoSpeechTimer.setRepeats(true);

        this.favorabilityTimer = new Timer(300000, e -> { // 5分钟
            logger.info("5分钟计时器到期，好感度 -5");
            updateLikeabilityAsync(-5);
            sayRandomly();
        });
        this.favorabilityTimer.setRepeats(true);

        this.inactivityTimer = new Timer(300000, e -> enterIdleDoubtState()); // 5分钟
        this.inactivityTimer.setRepeats(false);

        // 初始化UI和监听器
        createContextMenu();
        initListeners();

        // 开始异步加载数据和动画
        startLoadingProcess();
    }

    /**
     * 启动异步加载流程：
     * (后台) 获取宠物数据 (名称, 好感度)
     * (EDT) 启动 SwingWorker 加载动画
     */
    private void startLoadingProcess() {
        new Thread(() -> {
            logger.info("正在从服务器加载初始宠物数据...");
            Map<String, String> petData = apiClient.getPetData();

            if (petData != null && !petData.isEmpty()) {
                this.petName = petData.getOrDefault("name", "seia"); // 备用 "seia"
                try {
                    this.currentLikeability = Integer.parseInt(petData.getOrDefault("likeability", "100"));
                    if (this.currentLikeability > 100)
                        this.currentLikeability = 100;
                    this.lastClickTimeFromServer = Long.parseLong(petData.getOrDefault("lastClickTime", "0"));
                    logger.info("宠物数据加载成功: Name=" + petName + ", Likeability=" + currentLikeability);
                } catch (NumberFormatException e) {
                    logger.log(Level.SEVERE, "解析服务器宠物数据失败", e);
                }
            } else {
                logger.severe("无法从服务器获取宠物数据，使用默认值。");
            }

            // 切换回 EDT 启动 SwingWorker
            SwingUtilities.invokeLater(() -> {
                AnimationLoaderWorker loader = new AnimationLoaderWorker(this.petName, this.currentLikeability);
                loader.execute();
            });
        }, "PetData-Loader-Thread").start();
    }

    /**
     * 初始化所有自定义光标 (锤子, 活塞)
     */
    private void createCursors() {
        this.hammerCursor = createHammerCursor();
        this.PistonCursor = createPistonCursor();
        this.defaultCursor = Cursor.getDefaultCursor();
    }

    /**
     * 创建锤子光标
     */
    private Cursor createHammerCursor() {
        try {
            URL imageUrl = getClass().getResource("/tools/hammer.png");
            if (imageUrl == null)
                throw new IOException("Resource not found: /tools/hammer.png");

            Image cursorImage = ImageIO.read(imageUrl);
            Point hotspot = new Point(8, 8);
            return Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, hotspot, "hammer");
        } catch (IOException e) {
            logger.warning("加载 'hammer.png' 光标失败: " + e.getMessage());
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
    }

    /**
     * 创建活塞光标
     */
    private Cursor createPistonCursor() {
        try {
            URL imageUrl = getClass().getResource("/tools/piston.gif");
            if (imageUrl == null)
                throw new IOException("Resource not found: /tools/piston.gif");

            Image cursorImage = ImageIO.read(imageUrl);
            Point hotspot = new Point(8, 8);
            return Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, hotspot, "piston");
        } catch (IOException e) {
            logger.warning("加载 'piston.gif' 光标失败: " + e.getMessage());
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        }
    }

    /**
     * 创建右键菜单
     */
    private void createContextMenu() {
        contextMenu = new JPopupMenu();

        // 信息显示 (动态更新)
        final JMenuItem infoItem = new JMenuItem();
        infoItem.setEnabled(false);
        infoItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        contextMenu.add(infoItem);
        contextMenu.addSeparator();

        contextMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                String infoText = String.format("名称: %s | 好感度: %d", petName, currentLikeability);
                infoItem.setText(infoText);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        // 好感度测试
        cheatLikeabilityItem = new JMenuItem("likeability test:");
        cheatLikeabilityItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        cheatLikeabilityItem.setForeground(Color.ORANGE);
        cheatLikeabilityItem.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(PetWindow.this, "输入新的好感度:", currentLikeability);
            if (input == null)
                return;
            try {
                int newLikeability = Integer.parseInt(input);
                newLikeability = Math.max(0, Math.min(100, newLikeability)); // 限制 0-100
                int difference = newLikeability - currentLikeability;
                if (difference == 0)
                    return;
                logger.info("通过菜单请求好感度变化: " + difference);
                updateLikeabilityAsync(difference);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(PetWindow.this, "请输入有效的数字", "输入错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        contextMenu.add(cheatLikeabilityItem);

        // 拖放文件提示
        MoveFileMenuItem = new JMenuItem("<html>拖动桌面文件给我能提升好感!<br>试试'街头不良少年第1卷'!</html>");
        MoveFileMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        MoveFileMenuItem.setEnabled(true);
        MoveFileMenuItem.setForeground(UIManager.getColor("MenuItem.disabledForeground"));
        contextMenu.add(MoveFileMenuItem);

        // 特殊互动
        specialActionMenuItem = new JMenuItem("特殊互动 (准备就绪)");
        specialActionMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        specialActionMenuItem.setEnabled(false); // 等待动画加载
        specialActionMenuItem.setToolTipText("动画还未加载");
        specialActionMenuItem.addActionListener(e -> {
            if (!isSpecialActionOnCooldown) {
                performSpecialAction();
            }
        });
        contextMenu.add(specialActionMenuItem);

        // 锤子
        hammerMenuItem = new JMenuItem("敲击!");
        hammerMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        hammerMenuItem.setEnabled(false); // 等待动画加载
        hammerMenuItem.setToolTipText("动画还未加载");
        hammerMenuItem.addActionListener(e -> toggleHammerMode());
        contextMenu.add(hammerMenuItem);

        // 轻推
        pistonMenuItem = new JMenuItem("我推!");
        pistonMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        pistonMenuItem.setEnabled(false); // 等待动画加载
        pistonMenuItem.setToolTipText("动画还未加载");
        pistonMenuItem.addActionListener(e -> togglePistonMode());
        contextMenu.add(pistonMenuItem);

        contextMenu.addSeparator();

        // 语音选项 (主菜单)
        voiceOptionsMenu = new JMenu("语音选项");
        voiceOptionsMenu.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        // 静音
        muteMenuItem = new JCheckBoxMenuItem("静音");
        muteMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        muteMenuItem.setSelected(isMuted);
        muteMenuItem.addActionListener(e -> {
            isMuted = muteMenuItem.isSelected();
            audioManager.setMuted(isMuted);
            logger.info("设置静音: " + isMuted);
        });
        voiceOptionsMenu.add(muteMenuItem);

        // 音量 (子菜单)
        volumeSubMenu = new JMenu("调整音量");
        volumeSubMenu.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        JSlider volumeSlider = new JSlider(0, 100, (int) (currentVolume * 100)); // 滑动条
        volumeSlider.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        volumeSlider.setPreferredSize(new Dimension(150, 45));
        volumeSlider.setMajorTickSpacing(50);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.addChangeListener(e -> {
            int volumePercent = volumeSlider.getValue();
            if (volumePercent != (int) (this.currentVolume * 100)) {
                this.currentVolume = volumePercent / 100.0f;
                audioManager.setVolume(this.currentVolume); // 同步到音频管理器
                logger.info("音量通过滑动条设置为: " + volumePercent + "%");
            }
        });
        volumeSubMenu.add(volumeSlider);
        voiceOptionsMenu.add(volumeSubMenu);

        // 显示气泡
        showBubbleMenuItem = new JCheckBoxMenuItem("显示气泡");
        showBubbleMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        showBubbleMenuItem.setSelected(!isBubbleDisabled);
        showBubbleMenuItem.addActionListener(e -> {
            isBubbleDisabled = !showBubbleMenuItem.isSelected();
            logger.info("设置气泡显示: " + !isBubbleDisabled);
            if (isBubbleDisabled) {
                speechBubble.setVisible(false); // 禁用时立即隐藏
            }
        });
        voiceOptionsMenu.add(showBubbleMenuItem);

        // 显示语言 (子菜单)
        languageSubMenu = new JMenu("显示语言");
        languageSubMenu.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        ButtonGroup langGroup = new ButtonGroup();
        JRadioButtonMenuItem langChinese = new JRadioButtonMenuItem("中文 (简体)");
        langChinese.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        langChinese.setSelected(true); // 默认中文
        langChinese.addActionListener(e -> logger.info("语言切换到: 中文 (简体)"));
        langGroup.add(langChinese);
        languageSubMenu.add(langChinese);
        voiceOptionsMenu.add(languageSubMenu);

        contextMenu.add(voiceOptionsMenu);
        contextMenu.addSeparator();

        // 退出
        exitMenuItem = new JMenuItem("退出");
        exitMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        exitMenuItem.addActionListener(e -> {
            logger.info("退出程序...");
            System.exit(0);
        });
        contextMenu.add(exitMenuItem);

        // (菜单自动隐藏逻辑)
        MouseAdapter autoHideListener = new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                // 检查鼠标是否仍在菜单或其子组件上
                Component source = e.getComponent();
                Point p = SwingUtilities.convertPoint(source, e.getPoint(), contextMenu);
                if (!contextMenu.contains(p)) {
                    // 检查是否在子菜单上 (例如音量滑动条)
                    for (Component comp : contextMenu.getComponents()) {
                        if (comp instanceof JMenu && ((JMenu) comp).isPopupMenuVisible()) {
                            JPopupMenu subMenu = ((JMenu) comp).getPopupMenu();
                            Point subP = SwingUtilities.convertPoint(source, e.getPoint(), subMenu);
                            if (subMenu.contains(subP)) {
                                return; // 鼠标在子菜单上，不隐藏
                            }
                        }
                    }
                    contextMenu.setVisible(false);
                }
            }
        };
        contextMenu.addMouseListener(autoHideListener);
        for (Component comp : contextMenu.getComponents()) {
            if (comp instanceof JMenuItem) {
                comp.addMouseListener(autoHideListener);
            }
        }
        // 特别为JMenu添加监听，因为它有子菜单
        voiceOptionsMenu.addMouseListener(autoHideListener);
        volumeSubMenu.addMouseListener(autoHideListener);
    }

    /**
     * 初始化事件监听器 (鼠标和拖放)
     */
    private void initListeners() {
        PetMouseListener mouseListener = new PetMouseListener();
        imageLabel.addMouseListener(mouseListener);
        imageLabel.addMouseMotionListener(mouseListener); // 监听拖动

        // 启用拖放
        imageLabel.setDropTarget(new DropTarget(imageLabel, new PetDropTargetListener()));
    }

    // ==================================
    // 动画资源加载 (由 SwingWorker 调用)
    // ==================================
    /**
     * 加载优先动画 (如 idle, happy, pickup)
     * 
     * @param petName      宠物名
     * @param favorability 当前好感度
     * @return 加载到的优先动画Map
     */
    private Map<String, List<BufferedImage>> loadPriorityAnimations(String petName, int favorability) {
        List<String> priorityNames = new ArrayList<>();
        priorityNames.add(getDefaultIdleAnimation(favorability)); // 必须先加载默认 idle
        priorityNames.add("happy");
        priorityNames.add("pickup");
        if (!priorityNames.contains("idle_normal")) {
            priorityNames.add("idle_normal"); // 确保 idle_normal 也被优先加载
        }
        logger.info("优先加载列表: " + priorityNames);
        return loadSpecificAnimations(petName, priorityNames);
    }

    /**
     * 从 mod/BA/[petName]/animations.properties 文件加载所有动画类型列表
     * * @return 动画名称列表
     */
    private List<String> listAnimationTypes() {
        // 默认备用列表
        List<String> defaultList = Arrays.asList("idle_normal", "idle_happy", "idle_unhappy", "idle_doubt", "idle_sad",
                "happy", "attack", "headache", "jump", "knockdown", "pickup", "skill");

        java.util.Properties props = new java.util.Properties();
        String resourcePath = "/mod/BA/" + this.petName + "/animations.properties";

        try (java.io.InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                props.load(is);
                String animationListStr = props.getProperty("animation_list");

                if (animationListStr != null && !animationListStr.isEmpty()) {
                    List<String> loadedList = Arrays.stream(animationListStr.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    if (!loadedList.isEmpty()) {
                        logger.info("成功从 " + resourcePath + " 加载 " + loadedList.size() + " 个动画类型。");
                        return loadedList; // 成功
                    }
                }
                logger.warning(resourcePath + " 中 'animation_list' 键为空或无效。");
            } else {
                logger.warning("找不到配置文件: " + resourcePath);
            }
        } catch (java.io.IOException e) {
            logger.log(Level.SEVERE, "读取 " + resourcePath + " 配置文件时出错", e);
        }

        logger.warning("将使用硬编码的默认动画列表。");
        return defaultList; // 失败时返回默认列表
    }

    /**
     * 加载单个动画的所有帧 (带缩放)
     * 
     * @param petName       宠物名 (用于路径)
     * @param animationName 动画名 (如 "idle_normal")
     * @return 帧列表 (BufferedImage List), 失败返回 null
     */
    private List<BufferedImage> loadAnimationFrames(String petName, String animationName) {
        String basePath = "/mod/BA/" + petName + "/";
        List<BufferedImage> frames = new ArrayList<>();
        URL dirUrl = getClass().getResource(basePath + animationName);

        if (dirUrl == null) {
            logger.warning("找不到动画资源目录: " + basePath + animationName);
            return null;
        }

        try {
            int frameIndex = 0;
            while (true) {
                String frameFileName = String.format("%s%s/%04d.png", basePath, animationName, frameIndex);
                URL frameUrl = getClass().getResource(frameFileName);
                if (frameUrl == null) {
                    if (frameIndex == 0)
                        logger.warning("找不到动画 '" + animationName + "' 的第一帧: " + frameFileName);
                    break; // 找不到下一帧，加载完毕
                }

                BufferedImage originalImage = ImageIO.read(frameUrl);
                if (originalImage != null) {
                    // 缩放
                    int newWidth = Math.max(1, (int) (originalImage.getWidth() * scale));
                    int newHeight = Math.max(1, (int) (originalImage.getHeight() * scale));

                    Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = resizedImage.createGraphics();
                    g2d.drawImage(scaledImage, 0, 0, null);
                    g2d.dispose();
                    frames.add(resizedImage);
                } else {
                    logger.warning("无法读取动画帧: " + frameFileName);
                }
                frameIndex++;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "加载动画 '" + animationName + "' 时出错", e);
        }

        if (!frames.isEmpty()) {
            logger.info("成功加载动画 '" + animationName + "' (" + frames.size() + " 帧)");
            return frames;
        } else {
            logger.warning("动画 '" + animationName + "' 加载失败，帧列表为空。");
            return null;
        }
    }

    /**
     * 加载指定列表中的动画
     * * @param petName 宠物名
     * 
     * @param animationNames 动画名列表
     * @return 加载到的动画 Map
     */
    private Map<String, List<BufferedImage>> loadSpecificAnimations(String petName, List<String> animationNames) {
        Map<String, List<BufferedImage>> loadedAnims = new HashMap<>();
        for (String name : animationNames) {
            List<BufferedImage> frames = loadAnimationFrames(petName, name);
            if (frames != null) {
                loadedAnims.put(name, frames);
            }
        }
        return loadedAnims;
    }

    // ===========
    // 动画播放控制
    // ===========
    /**
     * 播放动画 (循环)
     * * @param name 动画名称
     */
    private void playAnimation(String name) {
        if (name == null || !animations.containsKey(name) || animations.get(name).isEmpty()) {
            logger.warning("尝试播放不存在或为空的动画: " + name + "，回退到 idle_normal");
            name = "idle_normal"; // 备用
            if (!animations.containsKey(name) || animations.get(name).isEmpty()) {
                logger.severe("错误：连 idle_normal 动画都找不到！");
                return;
            }
        }

        // 如果已在播放同名动画，则不重启
        if (name.equals(currentAnimationName) && animationTimer != null && animationTimer.isRunning()) {
            return;
        }

        stopAnimation(); // 停止当前动画
        currentAnimationName = name;
        currentFrameIndex = 0;
        List<BufferedImage> frames = animations.get(name);

        if (frames.isEmpty()) {
            logger.warning("动画 '" + name + "' 帧列表为空。");
            return;
        }

        setImageFrame(frames.get(0)); // 设置第一帧并调整窗口大小

        animationTimer = new Timer(47, e -> { // 约 21 FPS
            currentFrameIndex = (currentFrameIndex + 1) % frames.size();
            setImageFrame(frames.get(currentFrameIndex));
        });
        animationTimer.setRepeats(true);
        animationTimer.start();
        logger.fine("开始循环播放动画: " + name);
    }

    /**
     * 播放动画一次，然后回到默认的 idle 动画
     * * @param name 动画名称
     */
    public void playAnimationOnce(String name) {
        if (name == null || !animations.containsKey(name) || animations.get(name).isEmpty()) {
            logger.warning("尝试播放一次不存在或为空的动画: " + name);
            playAnimation(getDefaultIdleAnimation(currentLikeability)); // 确保回到 idle
            return;
        }
        // 拖动时 (pickup) 优先级更高，不打断
        if (isAnimationPlaying("pickup")) {
            logger.info("正在拖动 (pickup)，忽略一次性动画请求: " + name);
            return;
        }

        stopAnimation();
        currentAnimationName = name; // 标记正在播放一次性动画
        currentFrameIndex = 0;
        List<BufferedImage> frames = animations.get(name);

        setImageFrame(frames.get(0)); // 显示第一帧

        animationTimer = new Timer(47, e -> {
            currentFrameIndex++;
            if (currentFrameIndex < frames.size()) {
                setImageFrame(frames.get(currentFrameIndex));
            } else {
                stopAnimation(); // 播放完毕
                // 根据状态回到正确的 idle
                if (isIdleDoubt) {
                    playAnimation("idle_doubt");
                } else {
                    playAnimation(getDefaultIdleAnimation(currentLikeability));
                }
            }
        });
        animationTimer.setRepeats(true);
        animationTimer.start();
        logger.fine("开始播放一次性动画: " + name);
    }

    /**
     * 停止当前动画计时器
     */
    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
            logger.fine("停止动画: " + currentAnimationName);
        }
    }

    /**
     * 设置当前 JLabel 图像，并自动调整窗口大小
     * * @param frame 要显示的 BufferedImage
     */
    private void setImageFrame(BufferedImage frame) {
        if (frame == null) {
            logger.warning("尝试设置一个空的图像帧！");
            return;
        }

        imageLabel.setIcon(new ImageIcon(frame));
        // 仅在窗口大小与图像大小不匹配时调整
        if (getWidth() != frame.getWidth() || getHeight() != frame.getHeight()) {
            SwingUtilities.invokeLater(() -> {
                setSize(frame.getWidth(), frame.getHeight());
                revalidate();
                repaint();
            });
        }
    }

    // =========
    // 语音和气泡
    // =========

    /**
     * 触发宠物说话 (播放语音和显示气泡)
     * * @param audioKey 语音文件的键 (如 "ch0070_...")
     * 
     * @param text 气泡上显示的文字
     */
    public void say(String audioKey, String text) {
        // 播放音频 (如果不静音)
        if (!isMuted) {
            audioManager.play(audioKey);
        }

        // 显示气泡 (如果未禁用)
        if (isBubbleDisabled) {
            logger.info("宠物(气泡已禁用)说: " + text);
            return;
        }

        // 计算气泡显示时长
        long audioDurationMs = audioManager.getAudioDurationMs(audioKey);
        long textDurationMs = 2500 + (long) (text.length() * 200); // 估算阅读时间
        long finalDurationMs;

        if (audioDurationMs > 0) {
            // 优先使用音频的时长
            finalDurationMs = audioDurationMs;
        } else {
            // 如果音频时长无效，则回退到文本估算时长
            finalDurationMs = textDurationMs;
            logger.warning("音频 " + audioKey + " 时长为 0, 仅按文本计算时长。");
        }
        finalDurationMs = Math.max(finalDurationMs, 2500); // 至少显示 2.5 秒

        // 计算气泡位置
        Point windowLocation = getLocationOnScreen();
        // 锚点Y = 窗口Y + (视觉偏移 * 缩放)
        int visualPetTopY = windowLocation.y + (int) (PET_VISUAL_TOP_OFFSET * scale);

        // 显示气泡
        speechBubble.showBubble(text, (int) finalDurationMs, visualPetTopY);

        logger.info("宠物说: " + text + " (音频: " + audioKey + ", 最终: " + finalDurationMs + "ms)");
    }

    /**
     * 根据 Key 播放对应的语音和气泡
     * * @param key 语音键 (例如 "attack")
     */
    private void sayForKey(String key) {
        SpeechPair pair = audioManager.getSpeechPair(key);
        if (pair != null) {
            say(pair.getKey(), pair.getText());
        } else {
            logger.warning("sayForKey: 无法为 " + key + " 找到 SpeechPair。");
        }
    }

    /**
     * 随机说话 (过滤掉 eventOnlyKeys 中的事件语音)
     */
    private void sayRandomly() {
        int attempts = 0;
        int maxAttempts = 10;

        while (attempts < maxAttempts) {
            SpeechPair pair = audioManager.getRandomSpeechPair();
            if (pair == null) {
                logger.warning("sayRandomly: 无法获取随机语音配对。");
                return;
            }

            // 过滤事件专用语音
            if (eventOnlyKeys.contains(pair.getKey())) {
                logger.info("sayRandomly: 抽到事件语音 (" + pair.getKey() + ")，重抽...");
                attempts++;
                continue;
            }

            // 找到合适的，播放
            say(pair.getKey(), pair.getText());
            return;
        }
        logger.warning("sayRandomly: 尝试 " + maxAttempts + " 次均抽到事件语音，跳过。");
    }

    // ===========================
    // 交互逻辑 (API, 道具, 特殊动作)
    // ===========================

    /**
     * 异步向服务器发送更新好感度的请求
     * * @param changeAmount 好感度变化量 (正数或负数)
     */
    private void updateLikeabilityAsync(int changeAmount) {
        new Thread(() -> {
            logger.info("发送 UPDATE_LIKEABILITY 请求: " + changeAmount);
            Map<String, String> response = apiClient.updateLikeability(changeAmount);

            SwingUtilities.invokeLater(() -> {
                String status = response.get("status");
                logger.info("收到 UPDATE_LIKEABILITY 响应: " + status);

                if ("SUCCESS".equals(status)) {
                    try {
                        int newLikeability = Integer.parseInt(response.get("newLikeability"));
                        if (currentLikeability != newLikeability) {
                            currentLikeability = newLikeability;
                            logger.info("好感度已更新为: " + currentLikeability);

                            // 如果因好感度变化而退出疑惑状态
                            if (isIdleDoubt) {
                                isIdleDoubt = false;
                                playAnimation(getDefaultIdleAnimation(currentLikeability));
                                logger.info("好感度变化，退出 idle_doubt 状态。");
                            }
                            // 如果当前是待机动画，则根据好感度更新
                            else if (!isAnimationPlaying("pickup") && !isOneTimeAnimation(currentAnimationName)) {
                                playAnimation(getDefaultIdleAnimation(currentLikeability));
                            }
                        }
                    } catch (NumberFormatException | NullPointerException ex) {
                        logger.log(Level.SEVERE, "解析好感度更新响应失败", ex);
                    }
                } else {
                    logger.warning("更新好感度失败: " + response.getOrDefault("message", "未知错误"));
                }
            });
        }).start();
    }

    /**
     * 执行特殊互动 (播放动画, 加好感, 进入冷却)
     */
    private void performSpecialAction() {
        // 进入冷却
        isSpecialActionOnCooldown = true;
        specialActionMenuItem.setEnabled(false);
        specialActionMenuItem.setText("特殊互动 (冷却中...)");

        // 执行动作
        logger.info("执行特殊互动！播放 " + SPECIAL_ACTION_ANIMATION + ", 好感度 +" + SPECIAL_ACTION_LIKEABILITY_GAIN);
        playAnimationOnce(SPECIAL_ACTION_ANIMATION);
        sayForKey(SPECIAL_ACTION_AUDIO);
        updateLikeabilityAsync(SPECIAL_ACTION_LIKEABILITY_GAIN);

        // 启动冷却计时器
        if (specialActionCooldownTimer != null) {
            specialActionCooldownTimer.stop();
        }
        specialActionCooldownTimer = new Timer(SPECIAL_ACTION_COOLDOWN_MS, e -> {
            logger.info("特殊互动冷却完毕。");
            isSpecialActionOnCooldown = false;
            specialActionMenuItem.setEnabled(true);
            specialActionMenuItem.setText("特殊互动 (准备就绪)");
            ((Timer) e.getSource()).stop(); // 停止这个一次性计时器
        });
        specialActionCooldownTimer.setRepeats(false);
        specialActionCooldownTimer.start();
    }

    /**
     * 切换锤子模式
     */
    private void toggleHammerMode() {
        isHammerMode = !isHammerMode;

        if (isHammerMode && isPistonMode) {
            togglePistonMode(); // 退出活塞模式
        }

        if (isHammerMode) {
            setCursor(hammerCursor);
            hammerMenuItem.setText("取消锤子");
        } else {
            setCursor(defaultCursor);
            hammerMenuItem.setText("敲击!");
        }
        logger.info("锤子模式切换: " + isHammerMode);
    }

    /**
     * 切换活塞模式
     */
    private void togglePistonMode() {
        isPistonMode = !isPistonMode;

        if (isPistonMode && isHammerMode) {
            toggleHammerMode(); // 退出锤子模式
        }

        if (isPistonMode) {
            setCursor(PistonCursor);
            pistonMenuItem.setText("取消推动");
        } else {
            setCursor(defaultCursor);
            pistonMenuItem.setText("我推!");
        }
        logger.info("活塞模式切换: " + isPistonMode);
    }

    /**
     * (保留) 拖动文件模式 (目前由 PetDropTargetListener 实现)
     */
    private void toggleMoveFileMode() {
        // 此功能现在由 PetDropTargetListener 自动处理
    }

    // ===============
    // 状态与计时器管理
    // ===============
    /**
     * 重置用户闲置计时器 (在任何用户互动时调用)
     */
    private void resetInactivityTimer() {
        // 如果之前处于疑惑状态，现在恢复正常
        if (isIdleDoubt) {
            isIdleDoubt = false;
            // 仅在当前确实在播放 idle_doubt 时才切换
            if ("idle_doubt".equals(currentAnimationName)) {
                playAnimation(getDefaultIdleAnimation(currentLikeability));
            }
        }
        // 重启闲置计时器
        inactivityTimer.restart();
        logger.info("用户互动，重置闲置计时器。");
    }

    /**
     * (由 inactivityTimer 调用) 进入“疑惑”闲置状态
     */
    private void enterIdleDoubtState() {
        // 如果正在播放一次性动画或拖动，延迟进入
        if (isOneTimeAnimation(currentAnimationName) || "pickup".equals(currentAnimationName)) {
            logger.info("闲置计时器到期，但正在播放一次性动画，延迟进入 idle_doubt。");
            Timer delayTimer = new Timer(5000, e -> enterIdleDoubtState());
            delayTimer.setRepeats(false);
            delayTimer.start();
            return;
        }

        logger.info("5分钟无互动，进入 idle_doubt 状态。");
        isIdleDoubt = true;
        playAnimation("idle_doubt");
    }

    /**
     * (由 SwingWorker 调用) 检查刚加载的动画是否对应菜单项，若是则启用
     * * @param animationName 刚加载完成的动画名
     */
    private void checkAndEnableMenuItem(String animationName) {
        if (animationName == null)
            return;

        // 1. 特殊互动 (需要 "attack")
        if (animationName.equals(SPECIAL_ACTION_ANIMATION) && specialActionMenuItem != null) {
            specialActionMenuItem.setEnabled(true);
            specialActionMenuItem.setToolTipText(null);
            logger.info("UI: 'specialActionMenuItem' 已启用 (attack 加载完毕)。");
        }

        // 2. 锤子 (需要 "headache")
        if (animationName.equals("headache") && hammerMenuItem != null) {
            hammerMenuItem.setEnabled(true);
            hammerMenuItem.setToolTipText(null);
            logger.info("UI: 'hammerMenuItem' 已启用 (headache 加载完毕)。");
        }

        // 3. 轻推 (需要 "knockdown")
        if (animationName.equals("knockdown") && pistonMenuItem != null) {
            pistonMenuItem.setEnabled(true);
            pistonMenuItem.setToolTipText(null);
            logger.info("UI: 'pistonMenuItem' 已启用 (knockdown 加载完毕)。");
        }
    }

    // ============
    // 辅助工具方法
    // ============
    /**
     * 根据好感度获取默认的 idle 动画名称
     */
    private String getDefaultIdleAnimation(int likeability) {
        String animName;
        if (likeability >= 90) {
            animName = "idle_happy";
        } else if (likeability >= 70) {
            animName = "idle_normal";
        } else if (likeability >= 60) {
            animName = "idle_unhappy";
        } else if (likeability >= 30) {
            animName = "idle_ignore"; // 假设存在 (需要确保资源里有)
        } else {
            animName = "idle_sad";
        }

        // 备用检查：如果目标动画不存在，回退到 idle_normal
        if (!animations.containsKey(animName) && animations.containsKey("idle_normal")) {
            logger.warning("找不到动画 " + animName + "，回退到 idle_normal");
            return "idle_normal";
        }
        // 如果 idle_normal 也不存在 (加载初期)，则返回请求的名称
        if (!animations.containsKey(animName)) {
            return "idle_normal"; // 在加载完成前，默认请求 idle_normal
        }

        return animName;
    }

    /**
     * 检查是否是一次性播放的动画
     */
    private boolean isOneTimeAnimation(String animationName) {
        if (animationName == null)
            return false;
        switch (animationName) {
            case "happy":
            case "attack":
            case "headache":
            case "jump":
            case "skill":
            case "knockdown":
                return true;
            default:
                return false;
        }
    }

    /**
     * 检查当前是否正在播放指定名称的动画
     */
    private boolean isAnimationPlaying(String name) {
        return name != null && name.equals(currentAnimationName) && animationTimer != null
                && animationTimer.isRunning();
    }

    // ==========================
    // 内部类 (事件监听器, 加载器)
    // ==========================

    /**
     * 内部类：处理鼠标事件 (点击, 拖动)
     */
    private class PetMouseListener extends MouseAdapter {

        /**
         * 处理鼠标点击 (左键单击, 右键菜单, 道具模式)
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            resetInactivityTimer(); // 任何点击都重置闲置

            // 右键菜单
            if (SwingUtilities.isRightMouseButton(e)) {
                if (mousePressStart == null) { // 确保不是拖动释放
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
                return;
            }

            // (确保是单击, 且非拖动释放)
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1 && mousePressStart == null) {

                // 锤子模式点击
                if (isHammerMode) {
                    logger.info("锤子模式点击！");
                    playAnimationOnce("headache");
                    sayForKey("ch0070_tactic_defeat_2");
                    updateLikeabilityAsync(-5);
                    toggleHammerMode(); // 自动退出
                    return;
                }

                // 活塞模式点击
                if (isPistonMode) {
                    logger.info("活塞模式点击！");
                    playAnimationOnce("knockdown");
                    sayForKey("ch0070_tactic_defeat_1");
                    updateLikeabilityAsync(-7);
                    togglePistonMode(); // 自动退出
                    return;
                }

                // 普通左键单击 (API 交互)
                logger.info("检测到左键单击事件，发送 CLICK 请求...");
                new Thread(() -> {
                    Map<String, String> response = apiClient.sendClick();
                    SwingUtilities.invokeLater(() -> {
                        String status = response.get("status");
                        if (status == null) {
                            logger.severe("服务器 CLICK 响应无效");
                            JOptionPane.showMessageDialog(PetWindow.this, "与服务器通信时发生错误 (无效响应)", "错误",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        logger.info("收到 CLICK 响应: " + status);
                        switch (status) {
                            case "SUCCESS":
                                playAnimationOnce("happy");
                                sayForKey("ch0070_eventmission_login_2");
                                try {
                                    currentLikeability = Integer.parseInt(response.get("likeability"));
                                    lastClickTimeFromServer = Long.parseLong(response.get("lastClickTime"));
                                    logger.info("互动成功，新好感度: " + currentLikeability);
                                } catch (NumberFormatException | NullPointerException ex) {
                                    logger.log(Level.SEVERE, "解析服务器 SUCCESS 响应数据失败", ex);
                                }
                                break;
                            case "COOLDOWN":
                                try {
                                    long remainingTime = Long.parseLong(response.get("remainingTime"));
                                    long totalSeconds = remainingTime / 1000;
                                    long hours = totalSeconds / 3600;
                                    long minutes = (totalSeconds % 3600) / 60;
                                    long seconds = totalSeconds % 60;
                                    JOptionPane.showMessageDialog(PetWindow.this,
                                            String.format("需要等待 %d 小时 %d 分钟 %d 秒后才能再次互动。", hours, minutes, seconds),
                                            "冷却中", JOptionPane.INFORMATION_MESSAGE);
                                } catch (Exception ex) {
                                    logger.log(Level.SEVERE, "解析冷却时间失败", ex);
                                    JOptionPane.showMessageDialog(PetWindow.this,
                                            "正在冷却中", "冷却中", JOptionPane.INFORMATION_MESSAGE);
                                }
                                break;
                            case "ERROR":
                            default:
                                String message = response.getOrDefault("message", "发生未知错误");
                                logger.severe("服务器返回 CLICK 错误: " + message);
                                JOptionPane.showMessageDialog(PetWindow.this,
                                        "互动失败: " + message, "错误", JOptionPane.ERROR_MESSAGE);
                                break;
                        }
                    });
                }).start();
            }
        }

        /**
         * 鼠标按下 (用于拖动)
         */
        @Override
        public void mousePressed(MouseEvent e) {
            resetInactivityTimer();
            if (SwingUtilities.isLeftMouseButton(e) && !isHammerMode && !isPistonMode) {
                mousePressStart = e.getPoint(); // 记录拖动起始点
            }
        }

        /**
         * 鼠标释放 (用于拖动)
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                boolean wasDragging = (mousePressStart != null);
                mousePressStart = null; // 清除拖动起始点

                // 如果释放时是拖动状态 (pickup)，则切换回 idle
                if (wasDragging && "pickup".equals(currentAnimationName)) {
                    playAnimation(getDefaultIdleAnimation(currentLikeability));
                }
            }
        }

        /**
         * 鼠标拖动 (移动窗口)
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            resetInactivityTimer();
            // 必须是左键按下 (mousePressStart != null)，且非道具模式
            if (mousePressStart != null) {
                // 首次拖动时，切换到 "pickup" 动画
                if (!isAnimationPlaying("pickup")) {
                    playAnimation("pickup");
                }

                // 计算新窗口位置
                Point mouseLocation = e.getLocationOnScreen();
                Point newLocation = new Point(mouseLocation.x - mousePressStart.x, mouseLocation.y - mousePressStart.y);

                // 限制窗口在屏幕内
                GraphicsConfiguration gc = getGraphicsConfiguration();
                Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                Rectangle screenBounds = gc.getBounds();
                int minX = screenBounds.x + screenInsets.left;
                int minY = screenBounds.y + screenInsets.top;
                int maxX = screenBounds.x + screenBounds.width - screenInsets.right - getWidth();
                int maxY = screenBounds.y + screenBounds.height - screenInsets.bottom - getHeight();

                newLocation.x = Math.max(minX, Math.min(newLocation.x, maxX));
                newLocation.y = Math.max(minY, Math.min(newLocation.y, maxY));

                setLocation(newLocation);
            }
        }
    }

    /**
     * 内部类：处理文件拖放 (Drag and Drop) 事件
     * 负责接收文件，移入回收站，并触发 "skill" 动画和好感度。
     */
    private class PetDropTargetListener extends DropTargetAdapter {

        /**
         * 拖入时检查是否为文件
         */
        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_MOVE);
            } else {
                dtde.rejectDrag();
            }
        }

        /**
         * 拖动悬停时 (保险)
         */
        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_MOVE);
            } else {
                dtde.rejectDrag();
            }
        }

        /**
         * 释放 (Drop) 文件时处理
         */
        @Override
        public void drop(DropTargetDropEvent dtde) {
            resetInactivityTimer(); // 拖放文件算互动

            if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.rejectDrop();
                return;
            }

            // 检查系统是否支持 "移至回收站"
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                logger.warning("文件拖放被接收，但系统不支持 '移至回收站'。");
                JOptionPane.showMessageDialog(PetWindow.this,
                        "您的操作系统不支持 Java 自动移至回收站。\n文件未被删除。",
                        "功能不支持", JOptionPane.WARNING_MESSAGE);
                dtde.dropComplete(false);
                return;
            }

            dtde.acceptDrop(DnDConstants.ACTION_MOVE);
            Transferable transferable = dtde.getTransferable();

            try {
                // 获取文件列表
                @SuppressWarnings("unchecked")
                final List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                if (files.isEmpty()) {
                    dtde.dropComplete(false);
                    return;
                }

                // 将文件 I/O 操作放入后台线程，避免冻结 UI
                new Thread(() -> {
                    int filesMovedCount = 0;
                    int totalLikeabilityGained = 0;
                    boolean hasSpecialFile = false;

                    for (File file : files) {
                        try {
                            String fileNameWithExt = file.getName();
                            String fileNameWithoutExt = fileNameWithExt;
                            int lastDotIndex = fileNameWithExt.lastIndexOf('.');
                            if (lastDotIndex > 0) {
                                fileNameWithoutExt = fileNameWithExt.substring(0, lastDotIndex);
                            }

                            logger.info("后台：正在将文件移至回收站: " + file.getAbsolutePath());
                            Desktop.getDesktop().moveToTrash(file); // 执行 "移至回收站"

                            filesMovedCount++;

                            // 检查特殊文件
                            if ("街头混混系列第1本".equals(fileNameWithoutExt)) {
                                totalLikeabilityGained += 15;
                                hasSpecialFile = true;
                                logger.info("检测到特殊文件: " + fileNameWithExt + ", +15 好感度。");
                            } else {
                                totalLikeabilityGained += 2; // 普通文件
                            }

                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "后台：无法将文件移至回收站: " + file.getName(), e);
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(PetWindow.this,
                                        "无法将文件 '" + file.getName() + "' 移至回收站。",
                                        "移动失败", JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }

                    // 如果至少有一个文件成功移动
                    if (filesMovedCount > 0) {
                        logger.info("后台：" + filesMovedCount + " 个文件被接收，请求 'skill' 动画。");
                        logger.info("总好感度增加: " + totalLikeabilityGained);

                        final int finalTotalLikeabilityGained = totalLikeabilityGained;
                        final boolean wasSpecialFile = hasSpecialFile;

                        // 在 EDT 中播放动画并更新好感度
                        SwingUtilities.invokeLater(() -> {
                            playAnimationOnce("skill");
                            if (wasSpecialFile) {
                                sayForKey("ch0070_exweapon_get"); // 特殊语音
                            } else {
                                sayForKey("ch0070_growup_4"); // 普通语音
                            }
                            updateLikeabilityAsync(finalTotalLikeabilityGained); // 一次性更新
                        });
                    }
                }, "File-Trashing-Thread").start();

                dtde.dropComplete(true); // 立即报告完成

            } catch (UnsupportedFlavorException | IOException e) {
                logger.log(Level.SEVERE, "处理拖放文件时出错", e);
                dtde.dropComplete(false);
            }
        }
    }

    /**
     * 内部类：使用 SwingWorker 在后台加载动画
     * (后台) 加载优先动画
     * (EDT) 显示第一帧
     * (后台) 加载剩余动画
     * (EDT) 播放初始动画, 启动计时器
     */
    private class AnimationLoaderWorker extends SwingWorker<Void, BufferedImage> {

        private final String petName;
        private final int initialLikeability;
        private String initialAnimationName; // 存储要播放的第一个动画名

        public AnimationLoaderWorker(String petName, int initialLikeability) {
            this.petName = petName;
            this.initialLikeability = initialLikeability;
        }

        /**
         * 后台线程：加载动画
         */
        @Override
        protected Void doInBackground() throws Exception {
            // 加载优先动画
            logger.info("后台：开始加载优先动画...");
            Map<String, List<BufferedImage>> priorityAnimations = loadPriorityAnimations(petName, initialLikeability);
            animations.putAll(priorityAnimations);

            // 立即检查是否可启用菜单项
            SwingUtilities.invokeLater(() -> {
                for (String animName : priorityAnimations.keySet()) {
                    checkAndEnableMenuItem(animName);
                }
            });

            // 准备第一帧
            initialAnimationName = getDefaultIdleAnimation(initialLikeability);
            if (!animations.containsKey(initialAnimationName) || animations.get(initialAnimationName).isEmpty()) {
                logger.warning("期望的 idle 动画 '" + initialAnimationName + "' 加载失败，回退到 idle_normal");
                initialAnimationName = "idle_normal";
            }

            if (animations.containsKey(initialAnimationName) && !animations.get(initialAnimationName).isEmpty()) {
                BufferedImage firstFrame = animations.get(initialAnimationName).get(0);
                publish(firstFrame); // 发布第一帧到 process()
            } else {
                logger.severe("关键动画 (idle_normal) 加载失败");
                throw new RuntimeException("关键动画 (idle) 加载失败");
            }

            // 加载剩余动画
            logger.info("后台：开始逐个加载剩余动画...");
            List<String> allTypes = listAnimationTypes();
            List<String> remainingNames = allTypes.stream()
                    .filter(name -> !animations.containsKey(name)) // 过滤已加载的
                    .collect(Collectors.toList());

            logger.info("后台加载列表: " + remainingNames);

            for (String animationName : remainingNames) {
                List<BufferedImage> frames = loadAnimationFrames(petName, animationName);
                if (frames != null) {
                    animations.put(animationName, frames); // 立即放入缓存

                    // 每加载一个，就检查是否可启用菜单
                    final String loadedAnimName = animationName;
                    SwingUtilities.invokeLater(() -> checkAndEnableMenuItem(loadedAnimName));
                }
            }

            logger.info("后台：所有动画加载完毕。");

            // 备用逻辑) 确保 idle_normal 存在
            if (!animations.containsKey("idle_normal") || animations.get("idle_normal").isEmpty()) {
                logger.warning("警告：缺少基础动画 'idle_normal'！");
                String fallbackIdle = animations.keySet().stream().filter(k -> k.startsWith("idle_")).findFirst()
                        .orElse(animations.keySet().stream().findFirst().orElse(null));

                if (fallbackIdle != null) {
                    animations.put("idle_normal", animations.get(fallbackIdle));
                    logger.info("使用 '" + fallbackIdle + "' 作为 'idle_normal' 的备用。");
                } else {
                    logger.severe("错误：连备用动画也找不到！");
                }
            }

            return null;
        }

        /**
         * EDT 线程：处理 publish() 发布的帧 (用于显示第一帧)
         */
        @Override
        protected void process(List<BufferedImage> chunks) {
            BufferedImage firstFrame = chunks.get(chunks.size() - 1);
            if (firstFrame != null) {
                logger.info("接收到第一帧，正在显示宠物...");
                imageLabel.setText(null); // 清除 "加载中..."
                imageLabel.setIcon(new ImageIcon(firstFrame));
                setSize(firstFrame.getWidth(), firstFrame.getHeight()); // 调整窗口大小
                setLocationRelativeTo(null); // 再次居中
            }
        }

        /**
         * EDT 线程：doInBackground 完成时调用
         */
        @Override
        protected void done() {
            try {
                get(); // 检查后台是否抛出异常

                logger.info("所有动画加载完成。开始播放初始动画: " + initialAnimationName);
                playAnimation(initialAnimationName);

                // 启动所有自动计时器
                logger.info("启动自动说话计时器 (2分钟)...");
                autoSpeechTimer.start();
                logger.info("启动好感度及闲置计时器 (5分钟)...");
                favorabilityTimer.start();
                inactivityTimer.start();

            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "动画加载线程失败", e);
                imageLabel.setText("加载失败!");
                imageLabel.setIcon(null);
                setSize(150, 50);
                setLocationRelativeTo(null);
                String errorMsg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
                JOptionPane.showMessageDialog(PetWindow.this, "加载宠物动画失败: " + errorMsg, "加载错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}