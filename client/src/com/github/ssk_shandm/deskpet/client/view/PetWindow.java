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

public class PetWindow extends JWindow {

    private static final Logger logger = Logger.getLogger(PetWindow.class.getName());
    private final double scale = 0.5; // 图片缩放比例
    private final int PET_VISUAL_TOP_OFFSET = 120; // 气泡偏移量
    private final Map<String, List<BufferedImage>> animations = new java.util.concurrent.ConcurrentHashMap<>();
    private final JLabel imageLabel = new JLabel();
    private Timer animationTimer;
    private String currentAnimationName;
    private int currentFrameIndex;

    private Point mousePressStart; // 用于拖动窗口

    private JPopupMenu contextMenu; // 右键菜单
    private JMenuItem exitMenuItem;
    private JMenuItem cheatLikeabilityItem;
    private JMenuItem hammerMenuItem;
    private JMenuItem pistonMenuItem;
    private JMenuItem MoveFileMenuItem;
    private JMenuItem specialActionMenuItem;
    // 语音和气泡相关
    private JMenu voiceOptionsMenu; // 语音选项
    private JCheckBoxMenuItem muteMenuItem; // 是否静音
    private JMenu volumeSubMenu; // 调整音量子菜单
    private JCheckBoxMenuItem showBubbleMenuItem; // 是否显示气泡
    private JMenu languageSubMenu; // 显示语言 (子菜单)

    // 语音和气泡的状态
    private boolean isMuted = false;
    private float currentVolume = 0.3f; // 0.0f (静音) 到 1.0f (最大)
    private boolean isBubbleDisabled = false; // 气泡是否被禁用

    // 气泡窗口和音频管理器
    private SpeechBubble speechBubble;
    private final AudioManager audioManager;

    private Timer autoSpeechTimer; // 自动说话计时器

    private Timer specialActionCooldownTimer;

    private Cursor hammerCursor;
    private Cursor PistonCursor;
    private Cursor defaultCursor;
    private volatile boolean isHammerMode = false;
    private volatile boolean isPistonMode = false;
    private volatile boolean isSpecialActionOnCooldown = false;

    // 特殊互动的常量
    private static final int SPECIAL_ACTION_COOLDOWN_MS = 60000; // 冷却时间
    private static final String SPECIAL_ACTION_ANIMATION = "attack";
    private static final String SPECIAL_ACTION_AUDIO = "ch0070_battle_in_1";
    private static final int SPECIAL_ACTION_LIKEABILITY_GAIN = 10; // 每次互动增加的好感度

    private final ApiClient apiClient = new ApiClient();
    private String petName = "加载中...";
    private int currentLikeability = 100;
    private String currentStatus = "health";
    private long lastClickTimeFromServer = 0;

    // 正常语音中需过滤的
    private final java.util.Set<String> eventOnlyKeys = java.util.Set.of(
            "ch0070_tactic_defeat_1",
            "ch0070_tactic_defeat_2",
            "ch0070_battle_in_1",
            "ch0070_exweapon_get",
            "ch0070_growup_4");

    public PetWindow() {
        // 窗口基础设置
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0)); // 设置背景透明
        setLayout(new BorderLayout());
        add(imageLabel, BorderLayout.CENTER);

        // 设置初始加载状态
        imageLabel.setText("加载中...");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        setSize(150, 50); // 设置一个临时的加载窗口大小
        setLocationRelativeTo(null); // 窗口居中显示
        setVisible(true);

        createCursors(); // 初始化光标

        // 初始化音频和气泡组件
        this.audioManager = new AudioManager();

        this.speechBubble = new SpeechBubble(this, getGraphicsConfiguration());

        // 初始化自动说话计时器
        this.autoSpeechTimer = new Timer(120000, e -> sayRandomly());
        this.autoSpeechTimer.setRepeats(true); // 重复执行

        // 加载资源和初始化
        createContextMenu(); // 创建右键菜单
        initListeners(); // 初始化菜单项和鼠标事件

        // 异步加载数据和动画
        startLoadingProcess();

    }

    /**
     * 启动异步加载流程：
     * 后台线程获取初始宠物数据 (petName, likeability)
     * 获取数据后，在 EDT 启动 SwingWorker (AnimationLoaderWorker)
     * SwingWorker 负责加载动画并显示宠物
     */
    private void startLoadingProcess() {
        new Thread(() -> { // 使用后台线程避免阻塞 UI
            logger.info("正在从服务器加载初始宠物数据...");
            Map<String, String> petData = apiClient.getPetData();

            // 确保在后台线程解析数据
            if (petData != null && !petData.isEmpty()) {
                this.petName = petData.getOrDefault("name", "seia"); // 使用 "seia" 作为备用
                this.currentStatus = petData.getOrDefault("status", "health");
                try {
                    this.currentLikeability = Integer.parseInt(petData.getOrDefault("likeability", "100"));
                    this.lastClickTimeFromServer = Long.parseLong(petData.getOrDefault("lastClickTime", "0"));
                    logger.info("宠物数据加载成功: Name=" + petName + ", Status=" + currentStatus + ", Likeability="
                            + currentLikeability + ", LastClick=" + lastClickTimeFromServer);
                } catch (NumberFormatException e) {
                    logger.log(Level.SEVERE, "解析从服务器获取的宠物数据失败", e);
                    // petName 和 currentLikeability 将使用默认值
                }
            } else {
                logger.severe("无法从服务器获取宠物数据或返回数据为空。将使用默认值。");
                // petName 和 currentLikeability 将使用默认值
            }

            // 获取数据后，切换回 EDT 启动 SwingWorker
            SwingUtilities.invokeLater(() -> {
                // 启动动画加载器
                AnimationLoaderWorker loader = new AnimationLoaderWorker(this.petName, this.currentLikeability);
                loader.execute();
            });
        }, "PetData-Loader-Thread").start();
    }

    /**
     * 加载所有动画帧并进行缩放
     */
    // private void loadAnimations() {
    // String basePath = "/BA/seia/"; // 动画资源的基础路径
    // String[] animationNames = { "idle_normal", "idle_happy", "idle_unhappy",
    // "idle_doubt", "idle_sad",
    // "happy", "attack", "headache", "jump", "knockdown", "pickup", "skill" };

    // for (String name : animationNames) {
    // List<BufferedImage> frames = new ArrayList<>();
    // URL dirUrl = getClass().getResource(basePath + name);

    // if (dirUrl != null) {
    // try {
    // int frameIndex = 0;
    // while (true) {
    // String frameFileName = String.format("%s%s/%04d.png", basePath, name,
    // frameIndex);
    // URL frameUrl = getClass().getResource(frameFileName);
    // if (frameUrl == null) {
    // if (frameIndex == 0) {
    // logger.warning("找不到动画 '" + name + "' 的第一帧: " + frameFileName);
    // }
    // break;
    // }

    // BufferedImage originalImage = ImageIO.read(frameUrl);
    // if (originalImage != null) {
    // // 缩放图片
    // int newWidth = (int) (originalImage.getWidth() * scale);
    // int newHeight = (int) (originalImage.getHeight() * scale);
    // // 确保宽高至少为 1
    // newWidth = Math.max(1, newWidth);
    // newHeight = Math.max(1, newHeight);

    // Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight,
    // Image.SCALE_SMOOTH);
    // BufferedImage resizedImage = new BufferedImage(newWidth, newHeight,
    // BufferedImage.TYPE_INT_ARGB);
    // Graphics2D g2d = resizedImage.createGraphics();
    // g2d.drawImage(scaledImage, 0, 0, null);
    // g2d.dispose();
    // frames.add(resizedImage);
    // } else {
    // logger.warning("无法读取动画帧: " + frameFileName);
    // }
    // frameIndex++;
    // }
    // } catch (IOException e) {
    // logger.log(Level.SEVERE, "加载动画 '" + name + "' 时出错", e);
    // }
    // } else {
    // logger.warning("找不到动画资源目录: " + basePath + name);
    // }

    // if (!frames.isEmpty()) {
    // animations.put(name, frames);
    // logger.info("成功加载动画 '" + name + "' (" + frames.size() + " 帧)");
    // } else {
    // logger.warning("动画 '" + name + "' 加载失败，帧列表为空或找不到资源。");
    // }
    // }

    // if (animations.isEmpty()) {
    // logger.severe("错误：未能加载任何动画帧！程序可能无法正常显示。");
    // JOptionPane.showMessageDialog(null, "无法加载动画资源！请检查资源路径和文件。", "资源错误",
    // JOptionPane.ERROR_MESSAGE);

    // } else if (!animations.containsKey("idle_normal") ||
    // animations.get("idle_normal").isEmpty()) {
    // logger.warning("警告：缺少基础动画 'idle_normal'！");

    // String fallbackIdle = animations.keySet().stream().filter(k ->
    // k.startsWith("idle_")).findFirst()
    // .orElse(null);
    // if (fallbackIdle == null && !animations.isEmpty()) {
    // fallbackIdle = animations.keySet().iterator().next(); // 随便找一个
    // }
    // if (fallbackIdle != null) {
    // animations.put("idle_normal", animations.get(fallbackIdle));
    // logger.info("使用 '" + fallbackIdle + "' 作为 'idle_normal' 的备用。");
    // } else {
    // logger.severe("错误：连备用动画也找不到！");
    // }
    // }
    // }

    /**
     * 加载优先动画
     */
    private Map<String, List<BufferedImage>> loadPriorityAnimations(String petName, int favorability) {
        List<String> priorityNames = new ArrayList<>();
        priorityNames.add(getDefaultIdleAnimation(favorability)); // 必须先获取好感度
        priorityNames.add("happy");
        priorityNames.add("pickup");
        if (!priorityNames.contains("idle_normal")) {
            priorityNames.add("idle_normal");
        }
        logger.info("优先加载列表: " + priorityNames);
        return loadSpecificAnimations(petName, priorityNames);
    }

    /**
     * 返回所有已知的动画类型名称列表
     * * @return 动画名称列表
     */
    private List<String> listAnimationTypes() {
        // 默认的硬编码列表，作为加载失败时的备用
        List<String> defaultList = Arrays.asList("idle_normal", "idle_happy", "idle_unhappy", "idle_doubt", "idle_sad",
                "happy", "attack", "headache", "jump", "knockdown", "pickup", "skill");

        // 尝试从 .properties 文件动态加载
        java.util.Properties props = new java.util.Properties();
        String resourcePath = "mod/BA/" + this.petName + "/animations.properties"; // 动态路径

        // 使用 getResourceAsStream 来读取 JAR 包内的资源
        try (java.io.InputStream is = getClass().getResourceAsStream(resourcePath)) {

            if (is != null) {
                props.load(is); // 加载配置
                String animationListStr = props.getProperty("animation_list"); // 读取 "animation_list" 键

                if (animationListStr != null && !animationListStr.isEmpty()) {
                    // 按逗号分割字符串，并清除可能存在的前后空格
                    List<String> loadedList = Arrays.stream(animationListStr.split(","))
                            .map(String::trim) // 移除空格
                            .filter(s -> !s.isEmpty()) // 移除空字符串
                            .collect(Collectors.toList());

                    if (!loadedList.isEmpty()) {
                        logger.info("成功从 " + resourcePath + " 加载 " + loadedList.size() + " 个动画类型。");
                        return loadedList; // 返回从文件加载的列表
                    }
                }
                logger.warning("在 " + resourcePath + " 中找到了文件，但 'animation_list' 键为空或无效。");

            } else {
                // 找不到文件
                logger.warning("找不到配置文件: " + resourcePath);
            }

        } catch (java.io.IOException e) {
            logger.log(Level.SEVERE, "读取 " + resourcePath + " 配置文件时出错", e);
        }

        // 加载失败
        // 如果 try 块中因为任何原因（找不到文件、IO 异常、键不存在）失败了，
        // 就返回硬编码的默认列表以确保程序能继续运行。
        logger.warning("将使用硬编码的默认动画列表。");
        return defaultList;
    }

    /**
     * (新增) 专用于加载单个动画资源，供 loadSpecificAnimations 和 SwingWorker 调用
     * * @param petName 宠物名称 (用于路径)
     * 
     * @param animationName 要加载的动画名称
     * @return 加载到的帧列表，如果失败则返回 null
     */
    private List<BufferedImage> loadAnimationFrames(String petName, String animationName) {
        String basePath = "/mod/BA/" + petName + "/";
        List<BufferedImage> frames = new ArrayList<>();
        URL dirUrl = getClass().getResource(basePath + animationName);

        if (dirUrl != null) {
            try {
                int frameIndex = 0;
                while (true) {
                    String frameFileName = String.format("%s%s/%04d.png", basePath, animationName, frameIndex);
                    URL frameUrl = getClass().getResource(frameFileName);
                    if (frameUrl == null) {
                        if (frameIndex == 0) {
                            logger.warning("找不到动画 '" + animationName + "' 的第一帧: " + frameFileName);
                        }
                        break;
                    }

                    BufferedImage originalImage = ImageIO.read(frameUrl);
                    if (originalImage != null) {
                        // 缩放图片
                        int newWidth = (int) (originalImage.getWidth() * scale);
                        int newHeight = (int) (originalImage.getHeight() * scale);
                        // 确保宽高至少为 1
                        newWidth = Math.max(1, newWidth);
                        newHeight = Math.max(1, newHeight);

                        Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight,
                                Image.SCALE_SMOOTH);
                        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight,
                                BufferedImage.TYPE_INT_ARGB);
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
        } else {
            logger.warning("找不到动画资源目录: " + basePath + animationName);
        }

        if (!frames.isEmpty()) {
            logger.info("成功加载动画 '" + animationName + "' (" + frames.size() + " 帧)");
            return frames;
        } else {
            logger.warning("动画 '" + animationName + "' 加载失败，帧列表为空或找不到资源。");
            return null; // 返回 null 表示失败
        }
    }

    /**
     * 加载指定名称列表的动画帧并进行缩放 (已修改为使用 loadAnimationFrames)
     * * @param petName 宠物名称 (用于构建路径)
     * 
     * @param animationNames 要加载的动画名称列表
     * @return 加载到的动画 Map
     */
    private Map<String, List<BufferedImage>> loadSpecificAnimations(String petName, List<String> animationNames) {
        Map<String, List<BufferedImage>> loadedAnims = new HashMap<>();

        // (修改) 循环并调用新方法
        for (String name : animationNames) {
            List<BufferedImage> frames = loadAnimationFrames(petName, name);
            if (frames != null) { // (loadAnimationFrames 已经处理了日志)
                loadedAnims.put(name, frames);
            }
        }

        return loadedAnims;
    }

    /**
     * 创建右键菜单
     */
    private void createContextMenu() {
        contextMenu = new JPopupMenu();

        // 信息显示
        final JMenuItem infoItem = new JMenuItem();
        infoItem.setEnabled(false);
        infoItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        contextMenu.add(infoItem);
        contextMenu.addSeparator();

        // 动态信息更新
        contextMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                // petName 在启动时是 "加载中..."，加载完成后会更新为实际名称
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
        cheatLikeabilityItem = new JMenuItem("likeability test:");
        cheatLikeabilityItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        cheatLikeabilityItem.setBackground(contextMenu.getBackground());
        cheatLikeabilityItem.setForeground(Color.ORANGE);
        cheatLikeabilityItem.addActionListener(e -> {

            String input = JOptionPane.showInputDialog(PetWindow.this, "输入新的好感度:", currentLikeability);

            if (input == null) {
                return;
            }

            try {
                int newLikeability = Integer.parseInt(input);

                // if (newLikeability > 100) newLikeability = 100;
                // if (newLikeability <html 0) newLikeability = 0;

                int difference = newLikeability - currentLikeability;

                if (difference == 0) {
                    return;
                }

                // 自动更新 currentLikeability 和 idle 动画
                logger.info("通过菜单请求好感度变化: " + difference);
                updateLikeabilityAsync(difference);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(PetWindow.this, "请输入有效的数字", "输入错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        contextMenu.add(cheatLikeabilityItem);

        // 唯一增加好感提示
        MoveFileMenuItem = new JMenuItem("<html>拖动桌面文件给我能提升好感!<br>试试'街头不良少年第1卷'!</html>");
        MoveFileMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        MoveFileMenuItem.setToolTipText("");
        MoveFileMenuItem.setEnabled(true);
        MoveFileMenuItem.setForeground(UIManager.getColor("MenuItem.disabledForeground"));
        contextMenu.add(MoveFileMenuItem);

        // 训练
        specialActionMenuItem = new JMenuItem("特殊互动 (准备就绪)");
        specialActionMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        specialActionMenuItem.setEnabled(false);
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
        hammerMenuItem.setEnabled(false);
        hammerMenuItem.setToolTipText("动画还未加载");
        hammerMenuItem.addActionListener(e -> toggleHammerMode());
        contextMenu.add(hammerMenuItem);

        // 轻推
        pistonMenuItem = new JMenuItem("我推!");
        pistonMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        pistonMenuItem.setEnabled(false);
        pistonMenuItem.setToolTipText("动画还未加载");
        pistonMenuItem.addActionListener(e -> togglePistonMode());
        contextMenu.add(pistonMenuItem);

        contextMenu.addSeparator();

        // 语音选项
        voiceOptionsMenu = new JMenu("语音选项");
        voiceOptionsMenu.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        // 是否静音
        muteMenuItem = new JCheckBoxMenuItem("静音");
        muteMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        muteMenuItem.setSelected(isMuted);
        muteMenuItem.addActionListener(e -> {
            isMuted = muteMenuItem.isSelected();
            logger.info("设置静音: " + isMuted);
            // 将状态同步给音频管理器
            audioManager.setMuted(isMuted);
        });
        voiceOptionsMenu.add(muteMenuItem);

        // 调整音量 (子菜单滑动条)
        volumeSubMenu = new JMenu("调整音量");
        volumeSubMenu.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        // 创建一个 JSlider 滑动条
        JSlider volumeSlider = new JSlider(0, 100, (int) (currentVolume * 100));
        volumeSlider.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10)); // 菜单里的字体小一点
        volumeSlider.setPreferredSize(new Dimension(150, 45)); // 给滑动条一个合适的大小
        volumeSlider.setMajorTickSpacing(50); // 每 50 显示一个大刻度
        volumeSlider.setPaintTicks(true); // 显示刻度
        volumeSlider.setPaintLabels(true); // 显示 "0", "50", "100" 标签

        // 添加监听器, 实时响应拖动
        volumeSlider.addChangeListener(e -> {
            int volumePercent = volumeSlider.getValue();

            // 检查音量是否真的改变了, 避免在相同值上重复设置
            if (volumePercent != (int) (this.currentVolume * 100)) {

                // 将 0-100 转换为 0.0f -0f
                this.currentVolume = volumePercent / 100.0f;

                // 将音量设置同步给音频管理器
                audioManager.setVolume(this.currentVolume);

                logger.info("音量通过滑动条设置为: " + volumePercent + "%");
            }
        });

        // 将滑动条添加到 *子菜单*
        volumeSubMenu.add(volumeSlider);

        // 将子菜单添加到 *主菜单*
        voiceOptionsMenu.add(volumeSubMenu);

        // 是否显示气泡
        showBubbleMenuItem = new JCheckBoxMenuItem("显示气泡");
        showBubbleMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        showBubbleMenuItem.setSelected(!isBubbleDisabled); // 默认选中 (显示)
        showBubbleMenuItem.addActionListener(e -> {
            isBubbleDisabled = !showBubbleMenuItem.isSelected();
            logger.info("设置气泡显示: " + !isBubbleDisabled);
            if (isBubbleDisabled) {
                // 如果禁用了，立即隐藏当前的气泡
                speechBubble.setVisible(false);
            }
        });
        voiceOptionsMenu.add(showBubbleMenuItem);

        // 显示语言 (作为子菜单)
        languageSubMenu = new JMenu("显示语言");
        languageSubMenu.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        // 添加语言选项
        ButtonGroup langGroup = new ButtonGroup();
        JRadioButtonMenuItem langChinese = new JRadioButtonMenuItem("中文 (简体)");
        langChinese.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        langChinese.setSelected(true); // 默认中文
        langChinese.addActionListener(e -> logger.info("语言切换到: 中文 (简体)"));

        // JRadioButtonMenuItem langJapanese = new JRadioButtonMenuItem("日本語");
        // langJapanese.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        // langJapanese.addActionListener(e -> logger.info("语言切换到: 日本語"));

        // ButtonGroup 确保一次只能选一个
        langGroup.add(langChinese);
        // langGroup.add(langJapanese);

        languageSubMenu.add(langChinese);
        // languageSubMenu.add(langJapanese);

        voiceOptionsMenu.add(languageSubMenu);

        // 将新主菜单添加到右键菜单
        contextMenu.add(voiceOptionsMenu);

        contextMenu.addSeparator(); // 我加了一个分隔符，让退出更清晰

        // 退出
        exitMenuItem = new JMenuItem("退出");
        exitMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        exitMenuItem.addActionListener(e -> {
            System.out.println("退出...");
            System.exit(0);
        });
        contextMenu.add(exitMenuItem);

        // 菜单自动隐藏
        contextMenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (!contextMenu.contains(e.getPoint())) {
                    contextMenu.setVisible(false);
                }
            }
        });
        MouseAdapter childMouseListener = new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(),
                        contextMenu);
                if (!contextMenu.contains(p)) {
                    contextMenu.setVisible(false);
                }
            }
        };
        infoItem.addMouseListener(childMouseListener);
        cheatLikeabilityItem.addMouseListener(childMouseListener);
        hammerMenuItem.addMouseListener(childMouseListener);
        pistonMenuItem.addMouseListener(childMouseListener);
        specialActionMenuItem.addMouseListener(childMouseListener);
        exitMenuItem.addMouseListener(childMouseListener);
        voiceOptionsMenu.addMouseListener(childMouseListener);
    }

    /**
     * 异步从服务器加载初始宠物数据
     */
    // private void loadInitialPetData() {
    // new Thread(() -> { // 使用后台线程避免阻塞 UI
    // logger.info("正在从服务器加载初始宠物数据...");
    // Map<String, String> petData = apiClient.getPetData();

    // SwingUtilities.invokeLater(() -> { // 确保在 EDT 中更新 UI 或成员变量
    // if (petData != null && !petData.isEmpty()) {
    // this.petName = petData.getOrDefault("name", "未知");
    // this.currentStatus = petData.getOrDefault("status", "health");
    // try {
    // this.currentLikeability =
    // Integer.parseInt(petData.getOrDefault("likeability", "100"));
    // this.lastClickTimeFromServer =
    // Long.parseLong(petData.getOrDefault("lastClickTime", "0"));
    // // 根据好感度设置初始动画
    // playAnimation(getDefaultIdleAnimation(currentLikeability));
    // logger.info("宠物数据加载成功: Name=" + petName + ", Status=" + currentStatus + ",
    // Likeability="
    // + currentLikeability + ", LastClick=" + lastClickTimeFromServer);
    // } catch (NumberFormatException e) {
    // logger.log(Level.SEVERE, "解析从服务器获取的宠物数据失败", e);
    // JOptionPane.showMessageDialog(this, "无法解析服务器返回的宠物数据。", "数据错误",
    // JOptionPane.ERROR_MESSAGE);
    // playAnimation("idle_normal"); // 播放默认动画
    // }
    // } else {
    // logger.severe("无法从服务器获取宠物数据或返回数据为空。");
    // JOptionPane.showMessageDialog(this, "无法从服务器获取宠物数据，请检查服务器连接。", "连接错误",
    // JOptionPane.ERROR_MESSAGE);
    // playAnimation("idle_normal"); // 播放默认动画
    // }
    // // 数据加载完成后，根据当前（可能是默认）动画更新窗口大小
    // updateWindowSizeToCurrentFrame();
    // });
    // }).start();
    // }

    /**
     * (新增)
     * 初始化所有自定义光标
     */
    private void createCursors() {
        this.hammerCursor = createHammerCursor();
        this.PistonCursor = createPistonCursor(); // 保持你原来的 PistonCursor 变量名
        this.defaultCursor = Cursor.getDefaultCursor();
    }

    /**
     * 
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
            System.err.println("加载 'hammer.png' 光标失败，使用备用光标: " + e.getMessage());
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
    }

    /**
     * 
     * 创建活塞
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
            System.err.println("加载 'piston.gif' 光标失败，使用备用光标: " + e.getMessage());
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); // Hand cursor 更有意义
        }
    }

    /**
     * 初始化事件监听器
     */
    private void initListeners() {
        PetMouseListener mouseListener = new PetMouseListener();
        imageLabel.addMouseListener(mouseListener);
        imageLabel.addMouseMotionListener(mouseListener); // 监听拖动

        imageLabel.setDropTarget(new DropTarget(imageLabel, new PetDropTargetListener()));
    }

    /**
     * 异步向服务器发送更新好感度的请求
     * 
     * @param changeAmount 好感度变化量
     */
    private void updateLikeabilityAsync(int changeAmount) {
        new Thread(() -> { // 使用后台线程处理网络请求
            logger.info("发送 UPDATE_LIKEABILITY 请求: " + changeAmount);
            Map<String, String> response = apiClient.updateLikeability(changeAmount);
            SwingUtilities.invokeLater(() -> { // 在 EDT 中处理响应
                String status = response.get("status");
                logger.info("收到 UPDATE_LIKEABILITY 响应: " + status);
                if ("SUCCESS".equals(status)) {
                    try {
                        int newLikeability = Integer.parseInt(response.get("newLikeability"));
                        if (currentLikeability != newLikeability) {
                            currentLikeability = newLikeability;
                            logger.info("好感度已更新为: " + currentLikeability);
                            // 如果当前不是一次性动画或拖动动画，则根据好感度更新待机动画
                            if (!isAnimationPlaying("pickup") && !isOneTimeAnimation(currentAnimationName)) {
                                playAnimation(getDefaultIdleAnimation(currentLikeability));
                            }
                        }
                    } catch (NumberFormatException ex) {
                        logger.log(Level.SEVERE, "解析好感度更新响应失败: " + response.get("newLikeability"), ex);
                    } catch (NullPointerException ex) {
                        logger.log(Level.SEVERE, "服务器成功响应中缺少 newLikeability", ex);
                    }
                } else {
                    String errorMsg = response.getOrDefault("message", "未知错误");
                    logger.warning("更新好感度失败: " + errorMsg);
                    // 可以在此处添加用户提示，但频繁的错误提示可能影响体验
                    // JOptionPane.showMessageDialog(PetWindow.this, "更新好感度失败: " + errorMsg, "错误",
                    // JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    /**
     * 播放动画（循环）
     * 
     * @param name 动画名称
     */
    private void playAnimation(String name) {
        if (name == null || !animations.containsKey(name) || animations.get(name).isEmpty()) {
            logger.warning("尝试播放不存在或为空的动画: " + name + "，将播放 idle_normal");
            name = "idle_normal"; // 备用动画
            if (!animations.containsKey(name) || animations.get(name).isEmpty()) {
                logger.severe("错误：连 idle_normal 动画都找不到！无法播放任何动画。");
                // 可以设置一个默认的错误图像
                // imageLabel.setIcon(null);
                // imageLabel.setText("无法加载动画");
                // setSize(100, 50); // 设置一个小的默认大小
                return; // 无法播放任何动画
            }
        }

        // 如果已经在播放该动画并且计时器在运行，则不重新启动
        if (name.equals(currentAnimationName) && animationTimer != null && animationTimer.isRunning()) {
            return;
        }

        stopAnimation(); // 停止当前可能正在运行的动画
        currentAnimationName = name;
        currentFrameIndex = 0;
        List<BufferedImage> frames = animations.get(name);

        // 设置第一帧并调整窗口大小
        if (!frames.isEmpty()) {
            setImageFrame(frames.get(0));
        } else {
            logger.warning("动画 '" + name + "' 的帧列表为空，无法显示第一帧。");
            return; // 没有帧可以显示
        }

        // 创建并启动新的计时器
        animationTimer = new Timer(47, e -> { // 每 100 毫秒切换一帧
            currentFrameIndex = (currentFrameIndex + 1) % frames.size();
            setImageFrame(frames.get(currentFrameIndex));
        });
        animationTimer.setRepeats(true); // 确保计时器重复执行
        animationTimer.start();
        logger.fine("开始循环播放动画: " + name);
    }

    /**
     * 播放动画一次，然后回到默认的 idle 动画
     * 
     * @param name 动画名称
     */
    public void playAnimationOnce(String name) {
        if (name == null || !animations.containsKey(name) || animations.get(name).isEmpty()) {
            logger.warning("尝试播放一次不存在或为空的动画: " + name);
            // 播放失败也要确保回到 idle 状态
            playAnimation(getDefaultIdleAnimation(currentLikeability));
            return;
        }
        // 如果正在播放 pickup，则不打断
        if (isAnimationPlaying("pickup")) {
            logger.info("正在拖动 (pickup)，忽略一次性动画请求: " + name);
            return;
        }

        stopAnimation(); // 停止当前动画
        currentAnimationName = name; // 标记正在播放一次性动画
        currentFrameIndex = 0;
        List<BufferedImage> frames = animations.get(name);

        setImageFrame(frames.get(0)); // 显示第一帧

        animationTimer = new Timer(47, e -> {
            currentFrameIndex++;
            if (currentFrameIndex < frames.size()) {
                setImageFrame(frames.get(currentFrameIndex));
            } else {
                stopAnimation(); // 动画播放完毕
                // 确保回到正确的 idle 状态
                playAnimation(getDefaultIdleAnimation(currentLikeability));
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
            // 不重置 currentAnimationName，保留状态信息
        }
    }

    /**
     * 设置当前 JLabel 显示的图像帧，并根据需要调整窗口大小
     * 
     * @param frame 要显示的 BufferedImage
     */
    private void setImageFrame(BufferedImage frame) {
        if (frame != null) {
            imageLabel.setIcon(new ImageIcon(frame));
            // 仅在窗口大小与图像大小不匹配时调整
            if (getWidth() != frame.getWidth() || getHeight() != frame.getHeight()) {
                // 确保在 EDT 中调整大小
                SwingUtilities.invokeLater(() -> {
                    setSize(frame.getWidth(), frame.getHeight());
                    // pack(); // 使用 pack() 可能更可靠，但要确保布局管理器正确
                    revalidate();
                    repaint();
                });
            }
        } else {
            logger.warning("尝试设置一个空的图像帧！");
        }
    }

    /**
     * 根据默认动画 "idle_normal" 的第一帧设置窗口初始大小
     */
    // private void updateWindowSizeToDefaultAnimation() {
    // String defaultAnim = "idle_normal";
    // if (animations.containsKey(defaultAnim) &&
    // !animations.get(defaultAnim).isEmpty()) {
    // BufferedImage firstFrame = animations.get(defaultAnim).get(0);
    // setSize(firstFrame.getWidth(), firstFrame.getHeight());
    // } else {
    // logger.warning("无法找到默认动画 'idle_normal' 来设置初始窗口大小，使用默认值 150x150。");
    // setSize(150, 150); // 设置一个备用大小
    // }
    // }

    /**
     * 根据当前动画和帧索引更新窗口大小
     */
    // private void updateWindowSizeToCurrentFrame() {
    // if (currentAnimationName != null &&
    // animations.containsKey(currentAnimationName)) {
    // List<BufferedImage> frames = animations.get(currentAnimationName);
    // if (frames != null && !frames.isEmpty()) {
    // int index = (currentFrameIndex >= 0 && currentFrameIndex < frames.size()) ?
    // currentFrameIndex : 0;
    // BufferedImage frame = frames.get(index);
    // if (frame != null) {
    // SwingUtilities.invokeLater(() -> {
    // setSize(frame.getWidth(), frame.getHeight());
    // revalidate();
    // repaint();
    // });
    // }
    // }
    // }
    // }

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
        } else {
            animName = "idle_sad";
        }
        return animName;
    }

    /**
     * 检查指定名称的动画是否是一次性播放的动画
     */
    private boolean isOneTimeAnimation(String animationName) {
        // 使用 Set 可能更高效，但对于少量动画名，这样也可以
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
        // 检查计时器是否运行，并且当前动画名称匹配
        return name != null && name.equals(currentAnimationName) && animationTimer != null
                && animationTimer.isRunning();
    }

    /**
     * 拖动桌面程序或文件给宠物
     */
    private void toggleMoveFileMode() {

    }

    /**
     * 切换锤子模式
     */
    private void toggleHammerMode() {
        isHammerMode = !isHammerMode;

        // 如果进入锤子模式，确保退出活塞模式
        if (isHammerMode && isPistonMode) {
            togglePistonMode(); // 会自动设置回 defaultCursor
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

        // 如果进入活塞模式，确保退出锤子模式
        if (isPistonMode && isHammerMode) {
            toggleHammerMode(); // 会自动设置回 defaultCursor
        }

        if (isPistonMode) {
            //
            setCursor(PistonCursor);
            pistonMenuItem.setText("取消推动");
            // (来自我之前的逻辑)
            logger.info("进入活塞模式");
        } else {
            //
            setCursor(defaultCursor);
            pistonMenuItem.setText("我推!");
            // (来自我之前的逻辑)
            logger.info("退出活塞模式");
            // "attack" 动画
        }
    }

    /**
     * 执行特殊互动：播放动画、增加好感度，并开始冷却
     */
    private void performSpecialAction() {

        // 进入冷却状态
        isSpecialActionOnCooldown = true;
        specialActionMenuItem.setEnabled(false);
        // (可选) 动态更新文本以显示剩余时间，这里先用简单文本
        specialActionMenuItem.setText("特殊互动 (冷却中...)");

        // 执行动作
        logger.info("执行特殊互动！播放 '" + SPECIAL_ACTION_ANIMATION + "'，增加好感度 " + SPECIAL_ACTION_LIKEABILITY_GAIN);
        playAnimationOnce(SPECIAL_ACTION_ANIMATION);
        sayForKey(SPECIAL_ACTION_AUDIO);

        updateLikeabilityAsync(SPECIAL_ACTION_LIKEABILITY_GAIN);

        // 停止旧的计时器 (如果存在)
        if (specialActionCooldownTimer != null) {
            specialActionCooldownTimer.stop();
        }

        // 5. 启动一个新的、一次性的冷却计时器
        specialActionCooldownTimer = new Timer(SPECIAL_ACTION_COOLDOWN_MS, e -> {
            logger.info("特殊互动冷却完毕。");
            isSpecialActionOnCooldown = false;
            specialActionMenuItem.setEnabled(true);
            specialActionMenuItem.setText("特殊互动 (准备就绪)");

            // 停止这个一次性计时器
            ((Timer) e.getSource()).stop();
        });

        specialActionCooldownTimer.setRepeats(false); // 确保计时器只运行一次
        specialActionCooldownTimer.start();
    }

    /**
     * 请求 AudioManager 获取一个随机配对, 过滤掉黑名单中的 Key, 并执行 say()
     */
    private void sayRandomly() {
        int attempts = 0;
        int maxAttempts = 10; // 防止死循环

        while (attempts < maxAttempts) {
            SpeechPair pair = audioManager.getRandomSpeechPair();

            if (pair == null) {
                logger.warning("sayRandomly: 无法获取随机语音配对 (可能未加载)。");
                return; // 无法获取，直接退出
            }

            // 检查抽到的 Key 是否在我们的事件专用黑名单上
            if (eventOnlyKeys.contains(pair.getKey())) {
                logger.info("sayRandomly: 抽到了事件语音 (" + pair.getKey() + ")，正在重抽 (Attempt " + (attempts + 1) + ")");
                attempts++;
                continue;
            }

            // 找到了一个不在黑名单上的，正常播放
            say(pair.getKey(), pair.getText());
            return; // 播放后退出方法，结束循环
        }

        // 如果循环 10 次都失败了
        logger.warning("sayRandomly: 尝试了 " + maxAttempts + " 次，但都抽到了事件语音。本次跳过。");
    }

    /**
     * (新增)
     * 播放一个特定键的语音和气泡
     * 
     * @param key (例如 "attack")
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
     * (修改)
     * 触发宠物说话 (播放语音和显示气泡)
     * * @param audioKey 语音文件的键 (例如 "greeting")
     * 
     * @param text 气泡上显示的文字
     */
    public void say(String audioKey, String text) {
        // 检查气泡是否被禁用
        if (isBubbleDisabled) {
            // 仍然播放音频，只是不显示气泡
            if (!isMuted) {
                audioManager.play(audioKey);
            }
            logger.info("宠物(气泡已禁用)说: " + text);
            return; // 跳过所有气泡逻辑
        }

        if (!isMuted) {
            // 播放音频
            audioManager.play(audioKey);
        }

        // 获取音频时长
        long audioDurationMs = audioManager.getAudioDurationMs(audioKey);

        // 估算文字阅读时长
        // (例如: 基础 2.5 秒 + 200 毫秒/字)
        long textDurationMs = 2500 + (long) (text.length() * 200);

        // 为纯文字设置一个上限，避免过长
        // textDurationMs = Math.min(textDurationMs, 10000); // 比如文字最多显示 10 秒

        // 决定最终时长
        long finalDurationMs;
        if (audioDurationMs > 0) {
            // 如果有音频，使用 音频时长 和 文字时长 中 *较大* 的一个
            finalDurationMs = Math.max(audioDurationMs, textDurationMs);
            // 这样，如果音频 15s 而文字 3s，则显示 15s
            // 如果音频 1s 而文字 5s，则显示 5s
        } else {
            // 如果没有音频 (audioDurationMs <= 0)
            logger.warning("音频 " + audioKey + " 时长为 0, 仅按文本计算时长。");
            // 仅使用文字时长
            finalDurationMs = textDurationMs;
        }

        // 确保一个最小显示时间
        finalDurationMs = Math.max(finalDurationMs, 2500); // 无论如何，至少显示 2.5 秒

        // 计算气泡的锚点 Y 坐标
        // 获取窗口在屏幕上的位置
        Point windowLocation = getLocationOnScreen();
        // 计算宠物 "头顶" 的 Y 坐标 = 窗口 Y + (偏移量 * 缩放比例)
        int visualPetTopY = windowLocation.y + (int) (PET_VISUAL_TOP_OFFSET * scale);

        // 调用 showBubble, 传入计算出的时长
        speechBubble.showBubble(text, (int) finalDurationMs, visualPetTopY);

        logger.info("宠物说: " + text + " (音频: " + audioKey + ", 音频时长: " + audioDurationMs + "ms, 估算文字: " + textDurationMs
                + "ms, 最终: " + finalDurationMs + "ms)");
    }

    // 内部类：处理鼠标事件
    private class PetMouseListener extends MouseAdapter {

        /**
         * 处理鼠标点击事件（主要是左键单击和右键菜单）
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            // 右键弹出菜单
            if (SwingUtilities.isRightMouseButton(e)) {
                if (mousePressStart == null) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
                return; // 不执行后续的左键点击逻辑
            }

            // 锤子模式点击逻辑
            if (isHammerMode && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1
                    && mousePressStart == null) {
                logger.info("锤子模式点击！");
                playAnimationOnce("headache");
                sayForKey("ch0070_tactic_defeat_2");
                updateLikeabilityAsync(-5);

                // 点击后自动退出锤子模式
                toggleHammerMode();

                return; // 阻止执行下面的“普通点击”
            }

            // 推动模式点击逻辑
            if (isPistonMode && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1
                    && mousePressStart == null) {
                logger.info("活塞模式点击！");

                // 播放一个"推动"动画
                playAnimationOnce("knockdown");

                sayForKey("ch0070_tactic_defeat_1");

                // 推动一次减少好感度
                updateLikeabilityAsync(-7);

                // 点击后自动退出活塞模式
                togglePistonMode();

                return; // 阻止执行下面的“普通点击”
            }

            // 阻止活塞模式下的普通点击
            if (isPistonMode && SwingUtilities.isLeftMouseButton(e)) {
                logger.info("活塞模式点击，已忽略。");
                return;
            }

            // 普通左键单击 (逻辑不变)
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1 && mousePressStart == null) {
                logger.info("检测到左键单击事件，发送 CLICK 请求...");
                // (向服务器发送点击请求...)
                new Thread(() -> {
                    Map<String, String> response = apiClient.sendClick();
                    SwingUtilities.invokeLater(() -> {
                        String status = response.get("status");
                        if (status == null) {
                            logger.severe("服务器 CLICK 响应无效: status is null");
                            JOptionPane.showMessageDialog(PetWindow.this, "与服务器通信时发生错误 (无效响应)", "错误",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        logger.info("收到 CLICK 响应: " + status);
                        switch (status) {
                            case "SUCCESS":
                                playAnimationOnce("happy");
                                try {
                                    currentLikeability = Integer.parseInt(response.get("likeability"));
                                    lastClickTimeFromServer = Long.parseLong(response.get("lastClickTime"));
                                    logger.info("互动成功，新好感度: " + currentLikeability + ", 更新上次点击时间: "
                                            + lastClickTimeFromServer);
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
                                } catch (NumberFormatException | NullPointerException ex) {
                                    logger.log(Level.SEVERE, "解析冷却时间失败: " + response.get("remainingTime"), ex);
                                    JOptionPane.showMessageDialog(PetWindow.this,
                                            "正在冷却中（无法显示剩余时间）", "冷却中", JOptionPane.INFORMATION_MESSAGE);
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
         * 处理鼠标按下事件
         */
        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (!isHammerMode && !isPistonMode) {
                    mousePressStart = e.getPoint();
                }
            }
        }

        /**
         * 处理鼠标释放事件
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                boolean wasDragging = (mousePressStart != null);
                mousePressStart = null; // 清除拖动起始点

                // 如果之前是拖动状态 (播放 pickup)，切换回 idle
                if (wasDragging && "pickup".equals(currentAnimationName)) {
                    playAnimation(getDefaultIdleAnimation(currentLikeability));
                }
            }
        }

        /**
         * 处理鼠标拖动事件（移动窗口）
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            // 必须是左键按下状态，并且记录了起始点，且当前是 pickup 动画
            if (SwingUtilities.isLeftMouseButton(e) && mousePressStart != null) {
                // 检查：如果拖动开始了，但 "pickup" 还没播放，则开始播放
                if (!isAnimationPlaying("pickup")) {
                    playAnimation("pickup");
                }
                // 后续的拖动逻辑不变
                Point currentLocation = getLocation();
                Point mouseLocation = e.getLocationOnScreen(); // 获取鼠标在屏幕上的位置
                // 计算新窗口位置：当前鼠标屏幕位置 - 鼠标在窗口内的按下偏移
                Point newLocation = new Point(mouseLocation.x - mousePressStart.x, mouseLocation.y - mousePressStart.y);

                // 防止窗口移出屏幕可视范围 (可选，但推荐)
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
     * 使用 SwingWorker 在后台加载动画
     * Void: doInBackground 的返回类型 (我们不需要返回特定结果)
     * BufferedImage: process 方法的参数类型 (我们发布第一帧)
     */
    private class AnimationLoaderWorker extends SwingWorker<Void, BufferedImage> {

        private final String petName;
        private final int initialLikeability;
        private String initialAnimationName; // 存储要播放的第一个动画的名称

        public AnimationLoaderWorker(String petName, int initialLikeability) {
            this.petName = petName;
            this.initialLikeability = initialLikeability;
        }

        /**
         * 
         * 动画加载逻辑 (在后台线程执行)
         */
        @Override
        protected Void doInBackground() throws Exception {
            // 加载优先动画
            logger.info("后台：开始加载优先动画...");
            Map<String, List<BufferedImage>> priorityAnimations = loadPriorityAnimations(petName, initialLikeability);
            // (安全地放入 ConcurrentHashMap)
            animations.putAll(priorityAnimations);

            // (新) 立即检查已加载的优先动画，看是否能启用菜单项
            // 必须在 SwingUtilities.invokeLater 中执行，因为这是在后台线程
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
                // 发布第一帧，这将触发 EDT 上的 process() 方法
                publish(firstFrame);
            } else {
                // 严重错误：连 idle_normal 都加载失败
                logger.severe("关键动画 (idle_normal) 加载失败，无法启动宠物");
                throw new RuntimeException("关键动画 (idle) 加载失败，无法启动宠物");
            }

            // 开始逐个加载剩余的动画
            logger.info("后台：开始逐个加载剩余动画...");
            List<String> allTypes = listAnimationTypes();
            List<String> remainingNames = allTypes.stream()
                    .filter(name -> !animations.containsKey(name)) // 过滤掉已经加载的
                    .collect(Collectors.toList());

            logger.info("后台加载列表: " + remainingNames);

            // 循环，逐个加载并立即放入 map
            for (String animationName : remainingNames) {
                List<BufferedImage> frames = loadAnimationFrames(petName, animationName);

                if (frames != null) {
                    // 立即将加载好的动画放入线程安全的 map
                    // 这样 EDT 上的 playAnimationOnce 就能立刻访问到它
                    animations.put(animationName, frames);

                    // (新) 立即检查这个新加载的动画，看是否能启用菜单项
                    final String loadedAnimName = animationName; // 供 lambda 使用
                    SwingUtilities.invokeLater(() -> {
                        checkAndEnableMenuItem(loadedAnimName);
                    });
                }
            }
            // (旧的批量加载代码已被移除)

            logger.info("后台：所有动画加载完毕。");

            // 确保 idle_normal 存在 (备用逻辑)
            if (!animations.containsKey("idle_normal") || animations.get("idle_normal").isEmpty()) {
                logger.warning("警告：缺少基础动画 'idle_normal'！");
                String fallbackIdle = animations.keySet().stream().filter(k -> k.startsWith("idle_")).findFirst()
                        .orElse(null);
                if (fallbackIdle == null && !animations.isEmpty()) {
                    fallbackIdle = animations.keySet().iterator().next(); // 随便找一个
                }
                if (fallbackIdle != null) {
                    animations.put("idle_normal", animations.get(fallbackIdle));
                    logger.info("使用 '" + fallbackIdle + "' 作为 'idle_normal' 的备用。");
                } else {
                    logger.severe("错误：连备用动画也找不到！");
                }
            }

            return null; // doInBackground 返回 Void
        }

        /**
         * (新方法)
         * 在 EDT 执行，处理 publish() 发布的帧
         * 
         * @param chunks 发布的数据列表（这里是 BufferedImage）
         */
        @Override
        protected void process(List<BufferedImage> chunks) {
            // 获取最后发布的一帧（通常只有一帧）
            BufferedImage firstFrame = chunks.get(chunks.size() - 1);
            if (firstFrame != null) {
                logger.info("接收到第一帧，正在显示宠物...");
                imageLabel.setText(null); // 清除 "加载中..."
                imageLabel.setIcon(new ImageIcon(firstFrame));

                // 根据第一帧的大小调整窗口
                setSize(firstFrame.getWidth(), firstFrame.getHeight());
                // 调整大小后再次居中
                setLocationRelativeTo(null);
            }
        }

        /**
         * (新方法)
         * 在 EDT 执行，当 doInBackground 完成时调用
         */
        @Override
        protected void done() {
            try {
                get(); // 检查 doInBackground() 中是否抛出异常

                logger.info("所有动画加载完成。开始播放初始动画: " + initialAnimationName);
                playAnimation(initialAnimationName);

                // 启动自动说话计时器
                logger.info("启动自动说话计时器 (2分钟)...");
                autoSpeechTimer.start();

            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "动画加载线程失败", e);
                imageLabel.setText("加载失败!");
                imageLabel.setIcon(null);
                setSize(150, 50);
                setLocationRelativeTo(null);
                // (修正) 确保 getCause() 不为 null
                String errorMsg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
                JOptionPane.showMessageDialog(PetWindow.this, "加载宠物动画失败: " + errorMsg, "加载错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * (新增) 检查刚加载的动画是否对应某个菜单项，如果是，则在 EDT 中启用它
     * * @param animationName 刚刚加载完成的动画名称
     */
    private void checkAndEnableMenuItem(String animationName) {
        if (animationName == null) {
            return;
        }

        // 1. 检查 "特殊互动" (需要 "attack" 动画)
        if (animationName.equals(SPECIAL_ACTION_ANIMATION) && specialActionMenuItem != null) {
            specialActionMenuItem.setEnabled(true);
            specialActionMenuItem.setToolTipText(null); // 移除提示
            logger.info("UI: 'specialActionMenuItem' 已启用 (attack 动画加载完毕)。");
        }

        // 2. 检查 "锤子" (需要 "headache" 动画)
        if (animationName.equals("headache") && hammerMenuItem != null) {
            hammerMenuItem.setEnabled(true);
            hammerMenuItem.setToolTipText(null); // 移除提示
            logger.info("UI: 'hammerMenuItem' 已启用 (headache 动画加载完毕)。");
        }

        // 3. 检查 "轻推" (需要 "knockdown" 动画)
        if (animationName.equals("knockdown") && pistonMenuItem != null) {
            pistonMenuItem.setEnabled(true);
            pistonMenuItem.setToolTipText(null); // 移除提示
            logger.info("UI: 'pistonMenuItem' 已启用 (knockdown 动画加载完毕)。");
        }
    }

    /**
     * (新增)
     * 内部类：处理文件拖放 (Drag and Drop) 事件
     * * 负责接收从桌面拖拽的文件，将其移入回收站，并触发 "skill" 动画。
     */
    private class PetDropTargetListener extends DropTargetAdapter {

        /**
         * 当有东西拖入组件区域时调用
         */
        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            // 检查拖入的是否是文件列表
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                // 如果是文件，接受拖放，并显示 "移动" 图标
                dtde.acceptDrag(DnDConstants.ACTION_MOVE);
            } else {
                // 否则，拒绝
                dtde.rejectDrag();
            }
        }

        /**
         * 当拖动在组件区域上方移动时调用 (可选，但为了保险起见)
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
         * 当用户在组件上释放(放下)拖拽物时调用
         */
        @Override
        public void drop(DropTargetDropEvent dtde) {
            // 再次检查数据类型
            if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.rejectDrop();
                return;
            }

            // 接受拖放
            dtde.acceptDrop(DnDConstants.ACTION_MOVE);
            Transferable transferable = dtde.getTransferable();

            try {
                // 获取文件列表
                @SuppressWarnings("unchecked")
                final List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                if (files.isEmpty()) {
                    dtde.dropComplete(false); // 没有文件，操作失败
                    return;
                }

                // 检查系统是否支持 "移至回收站"
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                    logger.warning("文件拖放被接收，但系统不支持 '移至回收站'。");
                    JOptionPane.showMessageDialog(PetWindow.this,
                            "您的操作系统不支持 Java 自动移至回收站。\n文件未被删除。",
                            "功能不支持", JOptionPane.WARNING_MESSAGE);
                    dtde.dropComplete(false); // 操作未按预期完成
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
                            // 执行 "移至回收站"
                            Desktop.getDesktop().moveToTrash(file);

                            // 每成功一个，文件计数器+1
                            filesMovedCount++;

                            // 根据文件名累加好感度+
                            if ("街头混混系列第1本".equals(fileNameWithoutExt)) {
                                totalLikeabilityGained += 15;
                                hasSpecialFile = true;

                                // 日志里还是记录完整文件名，方便调试
                                logger.info("检测到特殊文件: " + fileNameWithExt + ", 增加 15 好感度。");
                            } else {
                                totalLikeabilityGained += 2;
                            }

                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "后台：无法将文件移至回收站: " + file.getName(), e);
                            // 在 EDT 中显示错误
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(PetWindow.this,
                                        "无法将文件 '" + file.getName() + "' 移至回收站。",
                                        "移动失败", JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }

                    // 检查计数器是否大于0 (即至少有一个文件成功移动)
                    if (filesMovedCount > 0) {
                        logger.info("后台：" + filesMovedCount + " 个文件已被接收，请求 'skill' 动画。");

                        final int finalTotalLikeabilityGained = totalLikeabilityGained;

                        final boolean wasSpecialFile = hasSpecialFile;

                        // (修改) 记录累加后的总好感度
                        logger.info("因接收 " + filesMovedCount + " 个文件，好感度请求增加: " + finalTotalLikeabilityGained);

                        // 在 EDT 中只播放动画并更新好感度
                        SwingUtilities.invokeLater(() -> {
                            playAnimationOnce("skill");
                            if (wasSpecialFile) {
                                // 如果收到了特殊文件，播放特殊语音
                                sayForKey("ch0070_exweapon_get");
                            } else {
                                // 否则，播放普通的 "skill" 语音
                                sayForKey("ch0070_growup_4");
                            }

                            // 一次性更新总的好感度
                            updateLikeabilityAsync(finalTotalLikeabilityGained); //
                        });
                    }
                }, "File-Trashing-Thread").start();

                // 立即报告 drop 完成 (因为我们已经接受了数据并启动了后台)
                dtde.dropComplete(true);

            } catch (UnsupportedFlavorException | IOException e) {
                // 处理获取数据时的异常
                logger.log(Level.SEVERE, "处理拖放文件时出错", e);
                dtde.dropComplete(false);
            }
        }
    }
}
