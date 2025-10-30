package com.github.ssk_shandm.deskpet.server.dao;

import com.github.ssk_shandm.deskpet.server.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据访问对象 (DAO)
 * 负责 users 表的所有数据库操作。
 */
public class UserDao {

    private static final Logger logger = Logger.getLogger(UserDao.class.getName());

    /**
     * 根据用户 ID 从数据库中查找用户
     * @param id 用户 ID (主键)
     * @return 如果找到，返回 User 对象；否则返回 null
     */
    public User findUser(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        User user = null;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    // user.setPoints(rs.getInt("points")); // points 字段已注释
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "按ID查询用户时出错", e);
        }
        return user;
    }

    /**
     * 将一个新的用户存入数据库 (ID 固定为 1)
     * @param user 要创建的用户对象 (仅使用 username)
     * @return 如果创建成功，返回 true；否则返回 false
     */
    public boolean createUser(User user) {
        // SQL 语句硬编码 id=1
        String sql = "INSERT INTO users (id, username) VALUES (1, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            // 特别处理 UNIQUE 约束冲突 (用户名已存在)
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                logger.warning("尝试创建用户失败：用户名 '" + user.getUsername() + "' 已存在。");
            } else {
                logger.log(Level.SEVERE, "创建用户时出错", e);
            }
            return false;
        }
    }
}