package com.github.ssk_shandm.deskpet.server.service;

import com.github.ssk_shandm.deskpet.server.dao.PetDao;
import com.github.ssk_shandm.deskpet.server.model.Pet;

/**
 * 负责处理所有与宠物相关的业务规则
 */
public class PetService {

    private final PetDao petDao = new PetDao();

    /**
     * 获取宠物信息
     *
     * @return Pet 宠物对象
     */
    public Pet getOrCreatePet() {
        // 从DAO获取宠物
        Pet pet = petDao.getPet();

        if (pet == null) {
            System.out.println("数据库中没有宠物，正在创建新的宠物...");
            pet = new Pet(1, 1, "Kami", "health"); // 创建一个默认宠物
            petDao.createPet(pet);
            System.out.println("新宠物创建成功！");
        }

        return pet;
    }

    /**
     * 更新宠物信息
     *
     * @param pet 包含最新信息的宠物对象
     * @return 如果更新成功，返回 true；否则返回 false
     */
    public boolean updatePet(Pet pet) {
        if (pet == null) {
            return false;
        }
        return petDao.updatePet(pet);
    }
}