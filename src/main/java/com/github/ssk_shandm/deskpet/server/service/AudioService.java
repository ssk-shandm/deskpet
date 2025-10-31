package com.github.ssk_shandm.deskpet.server.service;

import com.github.ssk_shandm.deskpet.server.dao.AudioDao;
import java.util.Map;

public class AudioService {

    private final AudioDao audioDao;

    public AudioService() {
        this.audioDao = new AudioDao();
    }

    /**
     * 获取所有缓存的时长
     */
    public Map<String, Long> getDurations() {
        return audioDao.getAllDurations();
    }

    /**
     * 更新一批时长
     */
    public void addDurations(Map<String, Long> newDurations) {
        if (newDurations != null && !newDurations.isEmpty()) {
            audioDao.saveDurations(newDurations);
        }
    }
}