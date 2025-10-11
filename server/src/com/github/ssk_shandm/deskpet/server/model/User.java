package com.github.ssk_shandm.deskpet.server.model;

import java.sql.Timestamp;

public class User {

    private int id;
    private String username;
    private String password;
    private Timestamp createdAt;
    private int points;

    public User() {
    }

    /**
     * @param id 用户ID
     * @param username 用户名
     * @param password 密码
     * @param createdAt 创建时间
     */
    public User(int id, String username, String password, Timestamp createdAt, int points) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.createdAt = createdAt;
        this.points =  points;
    }


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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}