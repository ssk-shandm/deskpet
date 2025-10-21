package com.github.ssk_shandm.deskpet.server.dao;

import com.github.ssk_shandm.deskpet.server.model.Pet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PetDao {

    /**
     * 从数据库获取唯一的宠物信息
     *
     * @return Pet 对象；如果数据库中还没有宠物，则返回 null
     */
    public Pet getPet() {
        String sql = "SELECT * FROM pets WHERE id = 1";
        Pet pet = null;

        try (Connection cc = DatabaseUtil.getConnection();
                PreparedStatement pstmt = cc.prepareStatement(sql)) {

            try (ResultSet rs = pstmt.executeQuery()) {

                // 如果查询到了结果
                if (rs.next()) {
                    pet = new Pet();
                    pet.setId(rs.getInt("id"));
                    pet.setName(rs.getString("name"));
                    pet.setStatus(rs.getString("status"));
                    pet.setLikeability(rs.getInt("likeability"));
                    pet.setLastClickTime(rs.getLong("last_click_time"));
                }
            }
        } catch (SQLException e) {
            System.err.println("查询宠物数据时出错: " + e.getMessage());
        }
        return pet;
    }

    /**
     * 将宠物信息更新到数据库
     */
    public boolean updatePet(Pet pet) {
        String sql = "UPDATE pets SET name = ?, status = ?, likeability = ?, last_click_time = ? WHERE id = 1";

        // 调试：数据库更新
        System.out.println("[PetDao Test] Name=" + pet.getName() + ", Status=" + pet.getStatus() + ", Likeability="
                + pet.getLikeability() + ", LastClickTime=" + pet.getLastClickTime());
        System.out.flush();

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 更新参数
            pstmt.setString(1, pet.getName());
            pstmt.setString(2, pet.getStatus());
            pstmt.setInt(3, pet.getLikeability());
            pstmt.setLong(4, pet.getLastClickTime());

            int affectedRows = pstmt.executeUpdate();

            // 调试：输出
            System.out.println("[PetDao Test] Update: " + affectedRows);
            System.out.flush();

            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("[PetDao ERROR] Update error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 首次创建宠物，并将其存入数据库
     * 只在检测到数据库中没有宠物时被调用一次
     */
    public boolean createPet(Pet pet) {
        String sql = "INSERT INTO pets (id, name,status, likeability, last_click_time) VALUES (1, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, pet.getName());
            pstmt.setString(2, pet.getStatus());
            pstmt.setInt(3, pet.getLikeability());
            pstmt.setLong(4, pet.getLastClickTime());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("创建宠物数据时出错: " + e.getMessage());
            return false;
        }
    }
}