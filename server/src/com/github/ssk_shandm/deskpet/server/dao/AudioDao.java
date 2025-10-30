package com.github.ssk_shandm.deskpet.server.dao;

import com.github.ssk_shandm.deskpet.server.model.AudioDuration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AudioDao {

    private static final Logger logger = Logger.getLogger(AudioDao.class.getName());

    /**
     * 从数据库加载所有已缓存的时长
     * 
     * @return Map<String, Long>
     */
    public Map<String, Long> getAllDurations() {
        Map<String, Long> durations = new HashMap<>();
        String sql = "SELECT key, duration_ms FROM audio_durations";

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                durations.put(rs.getString("key"), rs.getLong("duration_ms"));
            }
        } catch (SQLException e) {
            logger.severe("Failed to get all durations: " + e.getMessage());
        }
        return durations;
    }

    /**
     * 批量保存时长到数据库 (使用 INSERT OR REPLACE)
     * 
     * @param durationsMap 要保存的 Map
     */
    public void saveDurations(Map<String, Long> durationsMap) {
        String sql = "INSERT OR REPLACE INTO audio_durations (key, duration_ms) VALUES (?, ?)";

        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false); // 开启事务
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Map.Entry<String, Long> entry : durationsMap.entrySet()) {
                    pstmt.setString(1, entry.getKey());
                    pstmt.setLong(2, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit(); // 提交事务
                logger.info("Successfully saved " + durationsMap.size() + " duration entries.");
            } catch (SQLException e) {
                conn.rollback(); // 出错则回滚
                logger.severe("Failed to save durations batch: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.severe("Failed to get connection for saving durations: " + e.getMessage());
        }
    }
}