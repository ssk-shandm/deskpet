package com.github.ssk_shandm.deskpet.client.view;

import javax.sound.sampled.Clip;

/**
 * 数据类 (POJO)
 * 用于存储配对的音频 Clip 和对应的文案 String。
 */
public class SpeechPair {
    // 唯一标识键
    private final String key;
    // 预加载的音频剪辑
    private final Clip audioClip;
    // 对应的显示文本 
    private final String text;
    // 缓存的音频毫秒时长
    private final long durationMs;

    /**
     * 构造函数
     * * @param key        唯一标识键
     * @param audioClip  预加载的音频剪辑
     * @param text       对应的显示文本
     * @param durationMs 音频的毫秒时长
     */
    public SpeechPair(String key, Clip audioClip, String text, long durationMs) {
        this.key = key;
        this.audioClip = audioClip;
        this.text = text;
        this.durationMs = durationMs;
    }

    /**
     * @return 标识键
     */
    public String getKey() {
        return key;
    }

    /**
     * @return 音频剪辑 (Clip)
     */
    public Clip getAudioClip() {
        return audioClip;
    }

    /**
     * @return 显示文本
     */
    public String getText() {
        return text;
    }

    /**
     * @return 音频的毫秒时长 (缓存的)
     */
    public long getDurationMs() {
        return durationMs;
    }
}