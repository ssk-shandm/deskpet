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
    private final Map<String, List<BufferedImage>> animations = new HashMap<>();
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

    private Cursor hammerCursor;
    private Cursor PistonCursor;
    private Cursor defaultCursor;
    private volatile boolean isHammerMode = false;
    private volatile boolean isPistonMode = false;

    private final ApiClient apiClient = new ApiClient();
    private String petName = "加载中..."; // 初始值
    private int currentLikeability = 100; // 默认值，会被服务器数据覆盖
    private String currentStatus = "health"; // 默认值，会被服务器数据覆盖
    private long lastClickTimeFromServer = 0; // 存储从服务器获取的上次点击时间

    public PetWindow() {
        // --- 窗口基础设置 ---
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0)); // 设置背景透明
        setLayout(new BorderLayout());
        add(imageLabel, BorderLayout.CENTER);

        // --- 设置初始加载状态 ---
        imageLabel.setText("加载中...");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        setSize(150, 50); // 设置一个临时的加载窗口大小
        setLocationRelativeTo(null); // 窗口居中显示
        setVisible(true);

        // --- (新增) 在此处调用 ---
        createCursors(); // <--- 添加这一行来初始化光标

        // --- 加载资源和初始化 (非数据依赖) ---
        createContextMenu(); // 创建右键菜单
        initListeners(); // 初始化事件监听器 (菜单项和鼠标事件)

        // --- 异步加载数据和动画 ---
        startLoadingProcess();

    }

    /**
     * 启动异步加载流程：
     * 1. 后台线程获取初始宠物数据 (petName, likeability)
     * 2. 获取数据后，在 EDT 启动 SwingWorker (AnimationLoaderWorker)
     * 3. SwingWorker 负责加载动画并显示宠物
     */
    private void startLoadingProcess() {
        new Thread(() -> { // 1. 使用后台线程避免阻塞 UI
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

            // 2. 获取数据后，切换回 EDT 启动 SwingWorker
            SwingUtilities.invokeLater(() -> {
                // 3. 启动动画加载器
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
     * (来自你的逻辑)
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
     * (新方法 - 从配置文件加载)
     * 返回所有已知的动画类型名称列表
     * * @return 动画名称列表
     */
    private List<String> listAnimationTypes() {
        // 默认的硬编码列表，作为加载失败时的备用
        List<String> defaultList = Arrays.asList("idle_normal", "idle_happy", "idle_unhappy", "idle_doubt", "idle_sad",
                "happy", "attack", "headache", "jump", "knockdown", "pickup", "skill");

        // 尝试从 .properties 文件动态加载
        java.util.Properties props = new java.util.Properties();
        String resourcePath = "/BA/" + this.petName + "/animations.properties"; // 动态路径

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

        // --- 加载失败 ---
        // 如果 try 块中因为任何原因（找不到文件、IO 异常、键不存在）失败了，
        // 就返回硬编码的默认列表以确保程序能继续运行。
        logger.warning("将使用硬编码的默认动画列表。");
        return defaultList;
    }

    /**
     * (新方法 - 逻辑来自原始的 loadAnimations)
     * 加载指定名称列表的动画帧并进行缩放
     * 
     * @param petName        宠物名称 (用于构建路径)
     * @param animationNames 要加载的动画名称列表
     * @return 加载到的动画 Map
     */
    private Map<String, List<BufferedImage>> loadSpecificAnimations(String petName, List<String> animationNames) {
        Map<String, List<BufferedImage>> loadedAnims = new HashMap<>();

        // 注意：这里假设路径是 /BA/{petName}/
        // 原始代码是 /BA/seia/。如果 petName 不是 "seia"，这里需要正确
        String basePath = "/BA/" + petName + "/";

        for (String name : animationNames) {
            List<BufferedImage> frames = new ArrayList<>();
            URL dirUrl = getClass().getResource(basePath + name); // 检查目录是否存在

            if (dirUrl != null) {
                try {
                    int frameIndex = 0;
                    while (true) {
                        String frameFileName = String.format("%s%s/%04d.png", basePath, name, frameIndex);
                        URL frameUrl = getClass().getResource(frameFileName);
                        if (frameUrl == null) {
                            if (frameIndex == 0) { // 如果连第一帧都找不到
                                logger.warning("找不到动画 '" + name + "' 的第一帧: " + frameFileName);
                            }
                            break; // 找不到更多帧了
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
                    logger.log(Level.SEVERE, "加载动画 '" + name + "' 时出错", e);
                }
            } else {
                logger.warning("找不到动画资源目录: " + basePath + name);
            }

            if (!frames.isEmpty()) {
                loadedAnims.put(name, frames);
                logger.info("成功加载动画 '" + name + "' (" + frames.size() + " 帧)");
            } else {
                logger.warning("动画 '" + name + "' 加载失败，帧列表为空或找不到资源。");
            }
        }

        return loadedAnims;
    }

    /**
     * 创建右键菜单
     */
    private void createContextMenu() {
        contextMenu = new JPopupMenu(); // 你的 'Menu'

        // --- 信息显示 (来自你的逻辑) ---
        final JMenuItem infoItem = new JMenuItem();
        infoItem.setEnabled(false);
        infoItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        contextMenu.add(infoItem);
        contextMenu.addSeparator();

        // --- 动态信息更新 (来自你的逻辑) ---
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

        // --- 好感度修改 (逻辑合并) ---
        cheatLikeabilityItem = new JMenuItem("likeability test:"); // 你的 'cheatlikeabilityItem'
        cheatLikeabilityItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12)); // 你的样式
        cheatLikeabilityItem.setBackground(contextMenu.getBackground()); // 你的样式
        cheatLikeabilityItem.setForeground(Color.ORANGE); // 你的样式

        cheatLikeabilityItem.addActionListener(e -> {
            // 你的交互方式
            String input = JOptionPane.showInputDialog(PetWindow.this, "输入新的好感度:", currentLikeability);

            if (input == null) {
                return; // 用户取消
            }

            try {
                int newLikeability = Integer.parseInt(input);

                // (移除了 0-100 的限制，以匹配 800+ 的好感度等级)
                // if (newLikeability > 100) newLikeability = 100;
                // if (newLikeability < 0) newLikeability = 0;

                int difference = newLikeability - currentLikeability;

                if (difference == 0) {
                    return;
                }

                // (使用我们之前的异步更新方法)
                // 它会在后台发送请求，并在成功后自动更新 currentLikeability 和 idle 动画
                logger.info("通过菜单请求好感度变化: " + difference);
                updateLikeabilityAsync(difference);

            } catch (NumberFormatException ex) {
                // 你的错误提示
                JOptionPane.showMessageDialog(PetWindow.this, "请输入有效的数字", "输入错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        contextMenu.add(cheatLikeabilityItem);

        // 唯一增加好感提示
        MoveFileMenuItem = new JMenuItem("拖动桌面文件给我能提升好感!"); // 你的 'hammerItem'
        MoveFileMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        MoveFileMenuItem.addActionListener(e -> toggleMoveFileMode());
        MoveFileMenuItem.setEnabled(false);
        contextMenu.add(MoveFileMenuItem);

        // --- 锤子 (来自你的逻辑) ---
        hammerMenuItem = new JMenuItem("敲击!"); // 你的 'hammerItem'
        hammerMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        hammerMenuItem.addActionListener(e -> toggleHammerMode()); // 你的监听器
        contextMenu.add(hammerMenuItem);

        // --- 轻推 (来自你的逻辑) ---
        pistonMenuItem = new JMenuItem("我推!"); // 你的 'pistonItem'
        pistonMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        pistonMenuItem.addActionListener(e -> togglePistonMode()); // 你的监听器
        contextMenu.add(pistonMenuItem);

        contextMenu.addSeparator(); // 我加了一个分隔符，让退出更清晰

        // --- 退出 (来自你的逻辑) ---
        exitMenuItem = new JMenuItem("退出"); // 你的 'exitMenu'
        exitMenuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12)); // (为你添加了字体统一样式)
        exitMenuItem.addActionListener(e -> {
            System.out.println("退出...");
            System.exit(0);
        });
        contextMenu.add(exitMenuItem);

        // --- 菜单自动隐藏 (来自你的逻辑) ---
        // (已为你修复变量名)
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
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), contextMenu);
                if (!contextMenu.contains(p)) {
                    contextMenu.setVisible(false);
                }
            }
        };
        infoItem.addMouseListener(childMouseListener);
        cheatLikeabilityItem.addMouseListener(childMouseListener);
        hammerMenuItem.addMouseListener(childMouseListener);
        pistonMenuItem.addMouseListener(childMouseListener); // (为你补上了 pistonItem 的监听)
        exitMenuItem.addMouseListener(childMouseListener);
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
     * (来自你的逻辑)
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
     * (来自你的逻辑)
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
        animationTimer = new Timer(27, e -> { // 每 100 毫秒切换一帧
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

        animationTimer = new Timer(27, e -> {
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
            // (来自你的逻辑)
            setCursor(PistonCursor);
            pistonMenuItem.setText("取消推动");
            // (来自我之前的逻辑)
            logger.info("进入活塞模式");
        } else {
            // (来自你的逻辑)
            setCursor(defaultCursor);
            pistonMenuItem.setText("我推!");
            // (来自我之前的逻辑)
            logger.info("退出活塞模式");
            // "attack" 动画
        }
    }

    // --- 内部类：处理鼠标事件 ---
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

            // --- (修改) 锤子模式点击逻辑 ---
            if (isHammerMode && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1
                    && mousePressStart == null) {
                logger.info("锤子模式点击！");
                playAnimationOnce("headache");
                updateLikeabilityAsync(-20); // 锤一下减少 20 好感度

                // (新增) 点击后自动退出锤子模式
                toggleHammerMode();

                return; // 阻止执行下面的“普通点击”
            }

            // --- (新增) 推动模式点击逻辑 (实现与锤子一样的效果) ---
            if (isPistonMode && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1
                    && mousePressStart == null) {
                logger.info("活塞模式点击！");

                // 播放一个"推动"动画 (你可以改成 "knockdown" 或 "jump" 等)
                playAnimationOnce("knockdown");

                // 推动一次减少好感度 (例如 -15)
                updateLikeabilityAsync(-15);

                // (新增) 点击后自动退出活塞模式
                togglePistonMode();

                return; // 阻止执行下面的“普通点击”
            }

            // (新增) 阻止活塞模式下的普通点击 (这段逻辑现在被上面的 if 块覆盖了，但保留也没问题)
            if (isPistonMode && SwingUtilities.isLeftMouseButton(e)) {
                logger.info("活塞模式点击，已忽略。");
                return;
            }

            // --- 普通左键单击 (逻辑不变) ---
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
         * 处理鼠标按下事件（用于拖动和 Shift 键活塞模式）
         */
        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                // 检查是否按下了 Shift 键以触发活塞模式（按下时触发一次）
                if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
                    // 如果当前不是活塞模式，则切换并减少好感度
                    if (!isPistonMode) {
                        togglePistonMode();
                        updateLikeabilityAsync(-10); // 假设活塞模式减少 10 好感度
                        // 可以在这里设置一个特殊的拖动光标或动画
                    }
                    // 如果已经是活塞模式，按下不重复触发
                }
                // (重要修改) 只有在非锤子和非活塞模式下，才触发 pickup 拖动
                else if (!isHammerMode && !isPistonMode) {
                    // 正常按下，记录起始点并播放 pickup 动画
                    mousePressStart = e.getPoint();
                    // 按下时播放 pickup 动画
                    playAnimation("pickup");
                }
                // (新增) 如果在锤子或活塞模式下按下，我们什么也不做
                // 这样 mousePressStart 保持为 null，
                // mouseReleased 不会错误地切换回 idle，
                // mouseClicked 也能正确检测到单击事件
            }
        }

        /**
         * 处理鼠标释放事件（停止拖动）
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
                // 如果是 Shift 键释放，并且处于活塞模式，可以选择在这里退出活塞模式
                // if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0 && isPistonMode) {
                // togglePistonMode();
                // }
            }
        }

        /**
         * 处理鼠标拖动事件（移动窗口）
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            // 必须是左键按下状态，并且记录了起始点，且当前是 pickup 动画
            if (SwingUtilities.isLeftMouseButton(e) && mousePressStart != null && isAnimationPlaying("pickup")) {
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
         * (来自你的逻辑)
         * 动画加载逻辑 (在后台线程执行)
         */
        @Override
        protected Void doInBackground() throws Exception {
            // 加载优先动画
            logger.info("后台：开始加载优先动画...");
            Map<String, List<BufferedImage>> priorityAnimations = loadPriorityAnimations(petName, initialLikeability);
            // 将加载的动画放入主 Map
            animations.putAll(priorityAnimations);

            // 准备第一帧
            initialAnimationName = getDefaultIdleAnimation(initialLikeability);
            if (!animations.containsKey(initialAnimationName) || animations.get(initialAnimationName).isEmpty()) {
                // 如果期望的 idle（如 idle_happy）加载失败，回退到 idle_normal
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
                // 抛出异常，让 done() 方法处理
                throw new RuntimeException("关键动画 (idle) 加载失败，无法启动宠物");
            }

            // 加载剩余的动画
            logger.info("后台：开始加载剩余动画...");
            List<String> allTypes = listAnimationTypes();
            List<String> remainingNames = allTypes.stream()
                    .filter(name -> !animations.containsKey(name)) // 过滤掉已经加载的
                    .collect(Collectors.toList());

            logger.info("后台加载列表: " + remainingNames);
            if (!remainingNames.isEmpty()) {
                Map<String, List<BufferedImage>> remainingAnimations = loadSpecificAnimations(petName, remainingNames);
                animations.putAll(remainingAnimations); // 添加到主 Map
            }

            logger.info("后台：所有动画加载完毕。");

            // 确保 idle_normal 存在 (从原 loadAnimations 移植来的备用逻辑)
            if (!animations.containsKey("idle_normal") || animations.get("idle_normal").isEmpty()) {
                logger.warning("警告：缺少基础动画 'idle_normal'！");
                // 尝试找一个存在的 idle 动画作为备用
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

    // ... (在 PetWindow 类的末尾, AnimationLoaderWorker 类的上方或下方)

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

                // (重要) 检查系统是否支持 "移至回收站"
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                    logger.warning("文件拖放被接收，但系统不支持 '移至回收站'。");
                    JOptionPane.showMessageDialog(PetWindow.this,
                            "您的操作系统不支持 Java 自动移至回收站。\n文件未被删除。",
                            "功能不支持", JOptionPane.WARNING_MESSAGE);
                    dtde.dropComplete(false); // 操作未按预期完成
                    return;
                }

                // (重要) 将文件 I/O 操作放入后台线程，避免冻结 UI
                new Thread(() -> {
                    // (修改) 不再使用 boolean fileMoved，而是使用计数器
                    int filesMovedCount = 0;

                    for (File file : files) {
                        try {
                            logger.info("后台：正在将文件移至回收站: " + file.getAbsolutePath());
                            // 执行 "移至回收站"
                            Desktop.getDesktop().moveToTrash(file);

                            // (修改) 每成功一个，计数器+1
                            filesMovedCount++;

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

                    // (修改) 检查计数器是否大于0
                    if (filesMovedCount > 0) {
                        logger.info("后台：" + filesMovedCount + " 个文件已被接收，请求 'skill' 动画。");

                        // (修改) 计算总共应增加的好感度
                        final int totalLikeabilityGained = filesMovedCount * 5; // 每个文件 +5

                        // (重要修正) 把日志记录移到 invokeLater 的外部
                        // 这行代码现在在后台线程执行，可以直接访问 filesMovedCount
                        logger.info("因接收 " + filesMovedCount + " 个文件，好感度请求增加: " + totalLikeabilityGained);

                        // 在 EDT 中只播放动画并更新好感度
                        SwingUtilities.invokeLater(() -> {
                            playAnimationOnce("skill"); // 动画只播放一次

                            // (修改) 一次性更新总的好感度
                            updateLikeabilityAsync(totalLikeabilityGained);

                            // (已移除) logger.info(...) 已被移到外面
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
