package com.github.ssk_shandm.deskpet.server.dao;

import com.github.ssk_shandm.deskpet.server.model.User;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.*;

public class UserDao {

    private static final Logger logger = Logger.getLogger(UserDao.class.getName());

    /**
     * 根据用户名查询用户
     * 
     * @param username 要查询的用户名
     * @return 如果找到，返回User对象；否则返回null
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        User user = null;

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String password = rs.getString("password");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    int points = rs.getInt("points");

                    user = new User(id, username, password, createdAt, points);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "根据用户名查询用户失败", e);
        }

        return user;
    }
}