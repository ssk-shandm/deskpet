package com.github.ssk_shandm.deskpet.client.view;

import javax.sound.sampled.*;

import com.github.ssk_shandm.deskpet.client.network.ApiClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 音频管理器
 * 负责加载、管理和播放配对的语音 (Clip) 及文案 (SpeechPair)。
 */
public class AudioManager {

    private static final Logger logger = Logger.getLogger(AudioManager.class.getName());

    /** 存储所有 SpeechPair (Key -> Pair) */
    private final Map<String, SpeechPair> speechMap = new HashMap<>();
    /** 用于快速随机抽取的 Key 列表 */
    private final List<String> keyList = new ArrayList<>();
    private final Random random = new Random();

    private boolean isMuted = false;
    private float currentVolume = 0.3f; // 0.0f (静音) - 1.0f (最大)
    private final ApiClient apiClient;

    /**
     * 构造函数, 初始化时自动加载语音数据。
     */
    public AudioManager(ApiClient apiClient) {
        this.apiClient = apiClient;

        // 1. 从服务器获取缓存
        Map<String, Long> cachedDurations = apiClient.getAudioDurations();

        // 2. 加载语音数据 (传入缓存)
        loadSpeechData("/audio/BA/seia/", cachedDurations);
        logger.info("音频管理器已初始化。");
    }

    // ======================
    // 公共 API：播放与控制
    // =======================
    /**
     * 播放指定的音频
     * 
     * @param key 音频的键 (例如 "ch0070_...")
     */
    public void play(String key) {
        if (isMuted) {
            logger.info("请求播放 " + key + "，但处于静音状态。");
            return;
        }

        SpeechPair pair = speechMap.get(key);
        if (pair == null) {
            logger.warning("请求播放一个不存在的音频: " + key);
            return;
        }

        Clip clip = pair.getAudioClip();
        if (clip == null) {
            logger.warning("音频 " + key + " 的 Clip 为 null。");
            return;
        }

        if (clip.isRunning()) {
            clip.stop(); // 停止当前播放
        }

        try {
            setClipVolume(clip, this.currentVolume); // 设置音量
        } catch (Exception e) {
            logger.log(Level.WARNING, "设置音量失败: " + key, e);
        }

        clip.setFramePosition(0); // 回到开头
        clip.start(); // 播放
        logger.info("正在播放音频 " + key + " (音量: " + this.currentVolume + ")");
    }

    /**
     * 设置是否静音
     * 
     * @param muted true 为静音
     */
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        logger.info("AudioManager: 静音设置为 " + muted);

        // 如果设为静音, 停止所有正在播放的
        if (muted) {
            for (SpeechPair pair : speechMap.values()) {
                if (pair.getAudioClip() != null && pair.getAudioClip().isRunning()) {
                    pair.getAudioClip().stop();
                }
            }
        }
    }

    /**
     * 设置音量
     * 
     * @param volume 音量 (0.0f to 1.0f)
     */
    public void setVolume(float volume) {
        this.currentVolume = volume;
        logger.info("AudioManager: 音量设置为 " + volume);
        // 注意: 音量在 play() 时动态应用, 而不是在此处遍历所有 Clip
    }

    // ==================
    // 公共 API：数据获取
    // ==================

    /**
     * 从已加载的列表中随机获取一个 SpeechPair
     * 
     * @return 随机的 SpeechPair, 如果列表为空则返回 null
     */
    public SpeechPair getRandomSpeechPair() {
        if (keyList.isEmpty()) {
            logger.warning("请求随机语音，但列表为空。");
            return null;
        }
        String randomKey = keyList.get(random.nextInt(keyList.size()));
        return speechMap.get(randomKey);
    }

    /**
     * 根据 Key 获取 SpeechPair
     * 
     * @param key 音频/文本的键
     * @return 对应的 SpeechPair, 找不到则返回 null
     */
    public SpeechPair getSpeechPair(String key) {
        SpeechPair pair = speechMap.get(key);
        if (pair == null) {
            logger.warning("请求 SpeechPair，但未找到 key: " + key);
        }
        return pair;
    }

    /**
     * 
     * 现在只从缓存的 SpeechPair 中读取
     */
    public long getAudioDurationMs(String key) {
        SpeechPair pair = speechMap.get(key);
        if (pair != null) {
            // 直接返回加载时缓存的时长
            return pair.getDurationMs();
        }
        logger.warning("请求 " + key + " 的时长, 但未找到 SpeechPair。");
        return 0;
    }

    // =======================
    // 私有实现：加载与辅助
    // =======================

    /**
     * [!! 已修改 !!]
     * 核心加载方法：
     * 1. 检查服务器缓存
     * 2. 如果未命中，本地计算
     * 3. 异步上传新计算的数据
     */
    private void loadSpeechData(String resourcePath, Map<String, Long> cachedDurations) {
        logger.info("开始加载语音数据... 已有 " + cachedDurations.size() + " 条缓存。");
        // 用于存储本次新计算的时长，以便回传给服务器
        Map<String, Long> newDurationsToUpload = new HashMap<>();

        // 加载清单文件 (speech.properties)
        Properties props = new Properties();
        String propertiesPath = resourcePath + "speech.properties";
        List<String> keysToLoad = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(propertiesPath)) {
            if (is == null) {
                logger.severe("找不到语音清单文件: " + propertiesPath);
                return;
            }
            props.load(is);
            String keys = props.getProperty("speech_keys");
            if (keys != null && !keys.isEmpty()) {
                keysToLoad.addAll(Arrays.asList(keys.split(",")));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "读取语音清单失败: " + propertiesPath, e);
            return;
        }

        // 遍历清单, 加载配对
        for (String key : keysToLoad) {
            key = key.trim();
            if (key.isEmpty())
                continue;

            try {
                String audioPath = resourcePath + key + ".wav";
                URL audioUrl = getClass().getResource(audioPath);
                if (audioUrl == null) {
                    logger.warning("找不到音频文件: " + audioPath);
                    continue;
                }

                long durationMs = 0;

                // [!! 自动化逻辑的核心 !!]
                if (cachedDurations.containsKey(key)) {
                    // 1. 下一次打开：直接从缓存读取
                    durationMs = cachedDurations.get(key);
                } else {
                    // 2. 第一次打开 (或新音频)：本地计算
                    logger.info("未命中缓存: " + key + "。 正在本地计算时长...");
                    durationMs = calculateDurationReliably(audioUrl); // 调用辅助方法

                    if (durationMs > 0) {
                        newDurationsToUpload.put(key, durationMs); // 标记，以便稍后上传
                    }
                }

                // 加载 .wav 音频 (Clip)
                Clip clip;
                try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(
                        new BufferedInputStream(audioUrl.openStream()))) {
                    clip = AudioSystem.getClip();
                    clip.open(audioStream);
                }

                // 加载 .txt 文本
                String textPath = resourcePath + key + ".txt";
                String textContent;
                try (InputStream textIs = getClass().getResourceAsStream(textPath);
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(textIs, StandardCharsets.UTF_8))) {
                    textContent = reader.readLine(); // 假设文案只有一行
                }
                if (textContent == null || textContent.isEmpty()) {
                    logger.warning("文案文件为空或找不到: " + textPath);
                    textContent = "..."; // 提供备用
                }

                // [!! 修正 !!] 使用包含时长的构造函数
                SpeechPair pair = new SpeechPair(key, clip, textContent, durationMs);
                speechMap.put(key, pair);
                keyList.add(key);
                logger.fine("成功加载语音配对: " + key);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "加载配对 '" + key + "' 失败", e);
            }
        }

        // [!! 自动化逻辑的收尾 !!]
        // 异步将本次新计算的条目批量写入数据库
        if (!newDurationsToUpload.isEmpty()) {
            logger.info("发现 " + newDurationsToUpload.size() + " 条新音频，正在后台上传到服务器...");
            apiClient.saveAudioDurationsAsync(newDurationsToUpload);
        }
    }

    /**
     * 辅助方法：使用 AudioFileFormat (可靠) 计算时长
     */
    private long calculateDurationReliably(URL audioUrl) {
        long durationMs = 0;
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(audioUrl.openStream()))) {
            // 必须使用这个 URL 版本的方法，因为它不需要文件系统访问
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(audioUrl);
            AudioFormat format = fileFormat.getFormat();
            long frameLength = fileFormat.getFrameLength();
            float frameRate = format.getFrameRate();

            if (frameLength > 0 && frameRate > 0) {
                // (帧数 * 1000.0) / 帧率 = 毫秒
                durationMs = (long) ((frameLength * 1000.0) / frameRate);
            } else {
                logger.warning("无法通过 AudioFileFormat 计算 " + audioUrl.getFile() + " 的时长 (frameLength/frameRate 为 0)");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "计算 " + audioUrl.getFile() + " 时长失败", e);
        }
        return durationMs;
    }

    /**
     * 辅助方法：设置 Clip 的音量
     * 
     * @param clip   要设置的 Clip
     * @param volume 线性音量 (0.0f - 1.0f)
     */
    private void setClipVolume(Clip clip, float volume) {
        // 必须通过 FloatControl (MASTER_GAIN) 来控制音量
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            // 将 0.0-1.0 的线性音量转换为分贝(dB)
            float dB = (volume <= 0.0001f) ? gainControl.getMinimum() : (float) (Math.log10(volume) * 20.0);

            // 确保 dB 在允许范围内
            dB = Math.max(gainControl.getMinimum(), Math.min(dB, gainControl.getMaximum()));

            gainControl.setValue(dB);
        } else {
            logger.warning("不支持 MASTER_GAIN 音量控制。");
        }
    }
}