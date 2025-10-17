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

            try(ResultSet rs = pstmt.executeQuery()){

            // 如果查询到了结果
            if (rs.next()) {
                pet = new Pet();
                pet.setId(rs.getInt("id"));
                pet.setName(rs.getString("name"));
                pet.setRank(rs.getInt("Rank"));
                pet.setStatus(rs.getString("Status"));
                pet.setLikeability(rs.getInt("likeability"));
            }}
        } catch (SQLException e) {
            System.err.println("查询宠物数据时出错: " + e.getMessage());
        }
        return pet;
    }

    /**
     * 将宠物信息更新到数据库
     */
    public boolean updatePet(Pet pet) {
        String sql = "UPDATE pets SET name = ?, rank = ?, status = ?, favorability = ? WHERE id = 1";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 更新参数
            pstmt.setString(1, pet.getName());
            pstmt.setInt(2, pet.getRank());
            pstmt.setString(3,pet.getStatus());
            pstmt.setInt(4, pet.getLikeability());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0; // 如果影响的行数大于0，说明更新成功

        } catch (SQLException e) {
            System.err.println("更新宠物数据时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 首次创建宠物，并将其存入数据库
     * 只在检测到数据库中没有宠物时被调用一次
     */
    public boolean createPet(Pet pet) {
        String sql = "INSERT INTO pets (id, name, rank, status, likeability) VALUES (1, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, pet.getName());
            pstmt.setInt(2, pet.getRank());
            pstmt.setString(3, pet.getStatus());
            pstmt.setInt(4, pet.getLikeability());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("创建宠物数据时出错: " + e.getMessage());
            return false;
        }
    }
}