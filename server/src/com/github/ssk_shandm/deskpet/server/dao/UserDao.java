package com.github.ssk_shandm.deskpet.server.dao;

import com.github.ssk_shandm.deskpet.server.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 负责 users 表的所有数据库操作
 */
public class UserDao {

    /**
     * 根据用户名从数据库中查找用户
     *
     * @param id 用户ID
     * @return 如果找到，返回 User 对象；否则返回 null
     */
    public User findUser(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        User user = null;

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                // user.setPassword(rs.getString("password"));
                user.setPoints(rs.getInt("points"));
            }
        } catch (SQLException e) {
            System.err.println("按ID查询用户时出错: " + e.getMessage());
        }
        return user;
    }

    /**
     * 将一个新的用户存入数据库
     *
     * @param user 要创建的用户对象，需要包含用户名和密码
     * @return 如果创建成功，返回 true；否则返回 false
     */
    public boolean createUser(User user) {
        String sql = "INSERT INTO users (id,username) VALUES (1,?)";

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            // pstmt.setString(2, user.getPassword());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            // System.err.println("创建用户时出错: " + e.getMessage());
            // 特别处理 UNIQUE 约束冲突的错误
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                // System.err.println("用户名 '" + user.getUsername() + "' 已存在。");
            }
            return false;
        }
    }
}
