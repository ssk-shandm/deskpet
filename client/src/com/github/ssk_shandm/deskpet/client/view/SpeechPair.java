package com.github.ssk_shandm.deskpet.client.view;

import javax.sound.sampled.Clip;

/**
 * 数据类 (POJO)
 * 用于存储配对的音频 Clip 和对应的文案 String
 */
public class SpeechPair {
    private final String key;
    private final Clip audioClip;
    private final String text;

    /**
     * 构造函数
     * @param key 唯一标识键
     * @param audioClip 预加载的音频剪辑
     * @param text 对应的显示文本
     */
    public SpeechPair(String key, Clip audioClip, String text) {
        this.key = key;
        this.audioClip = audioClip;
        this.text = text;
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
}