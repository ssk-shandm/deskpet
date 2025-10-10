package com.github.ssk_shandm.deskpet.server.model;

import java.security.Timestamp;

// 用户必有信息
public class User {

    private int id;
    private String username;
    private double points;
    private String password;
    private Timestamp createdA;

    public User() {
    }

    public User(int id, String username, double points, String password, Timestamp createdA) {
        this.id = id;
        this.username = username;
        this.points = points;
        this.password = password;
        this.createdA = createdA;
    }

    // Id
    public int getId() {
        return id;
    }
    public void setId(int id) {
        
    }
}


