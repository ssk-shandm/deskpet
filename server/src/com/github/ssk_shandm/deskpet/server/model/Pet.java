package com.github.ssk_shandm.deskpet.server.model;

/**
 * 宠物模型 (POJO)
 * 对应 "pets" 数据库表。
 */
public class Pet {

    private int id;
    private String name;
    private int likeability; // 好感度
    private long lastClickTime; // 上次点击时间 (毫秒时间戳)

    /**
     * 默认构造函数
     */
    public Pet() {
    }

    /**
     * 完整构造函数
     * @param id            宠物 ID
     * @param name          名字
     * @param likeability   好感度
     * @param lastClickTime 上次点击时间
     */
    public Pet(int id, String name, int likeability, long lastClickTime) {
        this.id = id;
        this.name = name;
        this.likeability = likeability;
        this.lastClickTime = lastClickTime;
    }

    // Getters 和 Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLikeability() {
        return likeability;
    }

    public void setLikeability(int likeability) {
        this.likeability = likeability;
    }

    public long getLastClickTime() {
        return lastClickTime;
    }

    public void setLastClickTime(long lastClickTime) {
        this.lastClickTime = lastClickTime;
    }
}