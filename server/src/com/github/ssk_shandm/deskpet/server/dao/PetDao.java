package com.github.ssk_shandm.deskpet.server.dao;

import com.github.ssk_shandm.deskpet.server.model.Pet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据访问对象 (DAO)
 * 负责 pets 表的所有数据库操作。
 */
public class PetDao {

    private static final Logger logger = Logger.getLogger(PetDao.class.getName());

    /**
     * 从数据库获取唯一的宠物信息 (固定 ID=1)
     * 
     * @return Pet 对象；如果数据库中还没有宠物，则返回 null
     */
    public Pet getPet() {
        String sql = "SELECT * FROM pets WHERE id = 1";
        Pet pet = null;

        try (Connection cc = DatabaseUtil.getConnection();
                PreparedStatement pstmt = cc.prepareStatement(sql)) {

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    pet = new Pet();
                    pet.setId(rs.getInt("id"));
                    pet.setName(rs.getString("name"));
                    // pet.setStatus(rs.getString("status"));
                    pet.setLikeability(rs.getInt("likeability"));
                    pet.setLastClickTime(rs.getLong("last_click_time"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "查询宠物数据时出错", e);
        }
        return pet;
    }

    /**
     * 将宠物信息更新到数据库 (固定 ID=1)
     * 
     * @param pet 包含最新数据的 Pet 对象
     * @return 更新成功返回 true, 否则返回 false
     */
    public boolean updatePet(Pet pet) {

        // SQL 语句，匹配参数：
        String sql = "UPDATE pets SET name = ?, likeability = ?, last_click_time = ? WHERE id = 1";

        // // 调试：数据库更新
        // System.out.println("[PetDao Test] Name=" + pet.getName() + ", Likeability="
        // + pet.getLikeability() + ", LastClickTime=" + pet.getLastClickTime());
        // System.out.flush();

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 更新参数 (已修正索引)
            pstmt.setString(1, pet.getName());
            // pstmt.setString(2, pet.getStatus()); // status 字段已注释
            pstmt.setInt(2, pet.getLikeability()); // 原为 3
            pstmt.setLong(3, pet.getLastClickTime()); // 原为 4

            int affectedRows = pstmt.executeUpdate();

            // // 输出
            // System.out.println("[PetDao Test] Update: " + affectedRows);
            // System.out.flush();

            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PetDao ERROR] 更新宠物数据时出错", e);
            return false;
        }
    }

    /**
     * 首次创建宠物 (固定 ID=1)
     * 
     * @param pet 要创建的 Pet 对象
     * @return 创建成功返回 true, 否则返回 false
     */
    public boolean createPet(Pet pet) {
        // 同样修正 SQL 语句以匹配参数
        String sql = "INSERT INTO pets (id, name, likeability, last_click_time) VALUES (1, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, pet.getName());
            // pstmt.setString(2, pet.getStatus());
            pstmt.setInt(2, pet.getLikeability());
            pstmt.setLong(3, pet.getLastClickTime());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "创建宠物数据时出错", e);
            return false;
        }
    }
}