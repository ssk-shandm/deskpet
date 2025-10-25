package com.github.ssk_shandm.deskpet.client.view;

import javax.sound.sampled.Clip;

/**
 * (新增)
 * 用于存储配对的音频 Clip 和对应的文案 String
 */
public class SpeechPair {
    private final String key;
    private final Clip audioClip;
    private final String text;

    public SpeechPair(String key, Clip audioClip, String text) {
        this.key = key;
        this.audioClip = audioClip;
        this.text = text;
    }

    public String getKey() {
        return key;
    }

    public Clip getAudioClip() {
        return audioClip;
    }

    public String getText() {
        return text;
    }
}