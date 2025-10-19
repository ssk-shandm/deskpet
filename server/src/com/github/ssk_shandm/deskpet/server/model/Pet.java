package com.github.ssk_shandm.deskpet.server.model;

public class Pet {

    private int id;
    private String name;
    private String status;
    private int likeability;

    public Pet() {
    }

    /**
     * @param id     id
     * @param name   名字
     * @param likeability 好感度
     * @param status 状态
     */
    public Pet(int id, String name, String status, int likeability) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.likeability = likeability;

    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // 好感度
    public int getLikeability() {
        return likeability;
    }
    public void setLikeability(int likeability) {
        this.likeability = likeability;
    }
}
