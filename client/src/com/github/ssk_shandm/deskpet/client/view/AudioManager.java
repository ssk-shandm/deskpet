package com.github.ssk_shandm.deskpet.client.view;

import javax.sound.sampled.*;
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
 * (重构)
 * 负责加载、管理和播放配对的语音及文案
 */
public class AudioManager {

    private static final Logger logger = Logger.getLogger(AudioManager.class.getName());

    // (修改) 存储 SpeechPair 对象
    private final Map<String, SpeechPair> speechMap = new HashMap<>();
    // (新增) 用于快速随机抽取的 Key 列表
    private final List<String> keyList = new ArrayList<>();
    private final Random random = new Random();

    private boolean isMuted = false;
    private float currentVolume = 0.8f; // 0.0f - 1.0f

    public AudioManager() {
        // (修改) 调用加载器
        loadSpeechData("/audio/BA/seia/"); // 你指定的路径
        logger.info("音频管理器已初始化。");
    }

    /**
     * (新增)
     * 核心加载方法：读取 .properties，然后配对加载 .wav 和 .txt
     * 
     * @param resourcePath 资源所在的基础路径 (例如 "/audio/BA/seia/")
     */
    private void loadSpeechData(String resourcePath) {
        // 1. 加载清单文件
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

        if (keysToLoad.isEmpty()) {
            logger.warning("语音清单 " + propertiesPath + " 为空。");
            return;
        }

        // 2. 遍历清单, 加载配对
        for (String key : keysToLoad) {
            key = key.trim();
            if (key.isEmpty())
                continue;

            try {
                // 2a. 加载 .wav
                String audioPath = resourcePath + key + ".wav";
                URL audioUrl = getClass().getResource(audioPath);
                if (audioUrl == null) {
                    logger.warning("找不到音频文件: " + audioPath);
                    continue; // 缺少音频，跳过此配对
                }

                Clip clip;
                try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(
                        new BufferedInputStream(audioUrl.openStream()))) {
                    clip = AudioSystem.getClip();
                    clip.open(audioStream);
                }

                // 2b. 加载 .txt
                String textPath = resourcePath + key + ".txt";
                String textContent;
                try (InputStream textIs = getClass().getResourceAsStream(textPath);
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(textIs, StandardCharsets.UTF_8))) {
                    textContent = reader.readLine(); // (假设文案只有一行)
                }
                if (textContent == null || textContent.isEmpty()) {
                    logger.warning("文案文件为空或找不到: " + textPath);
                    textContent = "..."; // 提供一个备用
                }

                // 3. 创建并存储
                SpeechPair pair = new SpeechPair(key, clip, textContent);
                speechMap.put(key, pair);
                keyList.add(key);
                logger.info("成功加载语音配对: " + key);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "加载配对 '" + key + "' 失败", e);
            }
        }
    }

    /**
     * (重构)
     * 播放指定的音频
     * 
     * @param key 音频的键 (例如 "greeting")
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

        // (重要) 确保 Clip 未在播放
        if (clip.isRunning()) {
            clip.stop();
        }

        // (重要) 设置音量
        try {
            setClipVolume(clip, this.currentVolume);
        } catch (Exception e) {
            logger.log(Level.WARNING, "设置音量失败: " + key, e);
        }

        // (重要) 播放
        clip.setFramePosition(0); // 回到开头
        clip.start(); // 播放
        logger.info("正在播放音频 " + key + " (音量: " + this.currentVolume + ")");
    }

    /**
     * (新增)
     * 辅助方法：设置 Clip 的音量
     */
    private void setClipVolume(Clip clip, float volume) {
        if (volume < 0.0f)
            volume = 0.0f;
        if (volume > 1.0f)
            volume = 1.0f;

        // (关键) 必须通过 FloatControl 来控制音量
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            // 将 0.0-1.0 的线性音量转换为分贝(dB)
            // (注意: 0.0f 会导致 -Infinity, 所以我们设置一个最小值)
            float dB = (volume <= 0.0001f) ? gainControl.getMinimum() : (float) (Math.log10(volume) * 20.0);

            // 确保 dB 在允许范围内
            dB = Math.max(gainControl.getMinimum(), Math.min(dB, gainControl.getMaximum()));

            gainControl.setValue(dB);
        } else {
            logger.warning("不支持 MASTER_GAIN 音量控制。");
        }
    }

    /**
     * (重构)
     * 设置是否静音
     */
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        logger.info("AudioManager: 静音设置为 " + muted);

        if (muted) {
            // (新增) 如果静音，停止所有正在播放的
            for (SpeechPair pair : speechMap.values()) {
                if (pair.getAudioClip() != null && pair.getAudioClip().isRunning()) {
                    pair.getAudioClip().stop();
                }
            }
        }
    }

    /**
     * (重构)
     * 设置音量
     * 
     * @param volume 音量 (0.0f to 1.0f)
     */
    public void setVolume(float volume) {
        this.currentVolume = volume;
        logger.info("AudioManager: 音量设置为 " + volume);

        // (注意) 我们不需要在这里更新所有 Clip，
        // 我们选择在 play() 方法中设置音量，这样更高效。
    }

    /**
     * (新增)
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
     * (新增)
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
     * (新增)
     * 获取指定 key 的音频剪辑的时长 (毫秒)
     * 
     * @param key 音频的键
     * @return 时长 (毫秒), 如果找不到则返回 0
     */
    public long getAudioDurationMs(String key) {
        SpeechPair pair = speechMap.get(key);
        if (pair != null && pair.getAudioClip() != null) {
            // clip.getMicrosecondLength() 返回微秒, 除以 1000 得到毫秒
            return pair.getAudioClip().getMicrosecondLength() / 1000;
        }
        logger.warning("请求 " + key + " 的时长, 但未找到 Clip。");
        return 0;
    }
}