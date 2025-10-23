package com.github.ssk_shandm.deskpet.server.service;

import com.github.ssk_shandm.deskpet.server.dao.PetDao;
import com.github.ssk_shandm.deskpet.server.model.Pet;

/**
 * 负责处理所有与宠物相关的业务规则
 */
public class PetService {

    private final PetDao petDao = new PetDao();
    // 定义冷却时间常量 (24小时，以毫秒为单位)
    private static final long HAPPY_INTERACTION_COOLDOWN = 24 * 60 * 60 * 1000;

    /**
     * 获取宠物信息
     */
    public Pet getOrCreatePet() {
        // 从DAO获取宠物
        Pet pet = petDao.getPet();

        if (pet == null) {
            System.out.println("数据库中没有宠物，正在创建新的宠物...");
            pet = new Pet(1, "seia", "health", 100, 0L); // 创建一个默认宠物
            petDao.createPet(pet);
            System.out.println("新宠物创建成功！");
        }

        return pet;
    }

    /**
     * 更新宠物信息
     */
    public boolean updatePet(Pet pet) {
        if (pet == null) {
            return false;
        }
        return petDao.updatePet(pet);
    }

    /**
     * 检查 "Happy" 互动是否冷却完毕
     * 
     * @return 如果可以互动返回 true，否则返回 false
     */
    public boolean canPerformHappyInteraction() {
        Pet pet = getOrCreatePet(); // 获取当前宠物状态
        if (pet == null) {
            // 理论上不应该发生，除非数据库有问题
            return false;
        }
        long lastTime = pet.getLastClickTime();
        long currentTime = System.currentTimeMillis();

        return (currentTime - lastTime >= HAPPY_INTERACTION_COOLDOWN);
    }

    /**
     * 记录一次成功的 "Happy" 互动时间
     * 
     * @return 如果更新成功返回 true，否则返回 false
     */
    public boolean recordHappyInteraction() {
        Pet pet = getOrCreatePet();
        if (pet == null) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        pet.setLastClickTime(currentTime);
        // 这里可以选择性地增加好感度等其他逻辑
        // pet.setLikeability(pet.getLikeability() + 1); // 示例：每次点击增加1点好感度
        return updatePet(pet); // 更新数据库
    }

    /**
     * 获取距离下次可以进行 "Happy" 互动还有多少毫秒
     * 
     * @return 剩余的冷却时间（毫秒），如果已经冷却完毕则返回 0
     */
    public long getHappyInteractionRemainingCooldown() {
        Pet pet = getOrCreatePet();
        if (pet == null) {
            return HAPPY_INTERACTION_COOLDOWN; // 无法获取宠物信息，假设还在冷却
        }
        long lastTime = pet.getLastClickTime();
        if (lastTime == 0) {
            return 0; // 从未互动过
        }
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastTime;

        if (elapsedTime >= HAPPY_INTERACTION_COOLDOWN) {
            return 0; // 冷却已结束
        } else {
            return HAPPY_INTERACTION_COOLDOWN - elapsedTime; // 返回剩余时间
        }
    }
}