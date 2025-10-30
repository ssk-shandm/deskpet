package com.github.ssk_shandm.deskpet.server.model;

/**
 * 用户模型 (POJO)
 * 对应 "users" 数据库表。
 */
public class User {

    private int id;
    private String username;
    private int points; // 积分

    /**
     * 默认构造函数
     */
    public User() {
    }

    /**
     * 完整构造函数
     * @param id       用户ID
     * @param username 用户名
     * @param points   积分
     */
    public User(int id, String username, int points) {
        this.id = id;
        this.username = username;
        this.points = points;
    }

    // Getters 和 Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}