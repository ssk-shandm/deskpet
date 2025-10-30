package com.github.ssk_shandm.deskpet.server.service;

import com.github.ssk_shandm.deskpet.server.dao.PetDao;
import com.github.ssk_shandm.deskpet.server.model.Pet;
import java.util.logging.Logger;

/**
 * 宠物服务
 * 负责处理所有与宠物相关的业务规则 (如冷却时间)。
 */
public class PetService {

    private final PetDao petDao = new PetDao();
    private static final Logger logger = Logger.getLogger(PetService.class.getName());

    /** 定义冷却时间常量 (24小时，以毫秒为单位) */
    private static final long HAPPY_INTERACTION_COOLDOWN = 24 * 60 * 60 * 1000;

    /**
     * 获取或创建唯一的宠物信息 (固定 ID=1)
     * @return Pet 对象
     */
    public Pet getOrCreatePet() {
        Pet pet = petDao.getPet();

        if (pet == null) {
            logger.info("数据库中没有宠物，正在创建新的默认宠物...");
            pet = new Pet(1, "seia", 100, 0L); // 默认宠物
            petDao.createPet(pet);
            logger.info("新宠物创建成功！");
        }

        return pet;
    }

    /**
     * 更新宠物信息
     * @param pet 包含最新数据的 Pet 对象
     * @return 更新成功返回 true, 否则返回 false
     */
    public boolean updatePet(Pet pet) {
        if (pet == null) {
            return false;
        }
        return petDao.updatePet(pet);
    }

    /**
     * 检查 "Happy" 互动是否冷却完毕
     * @return 如果可以互动返回 true，否则返回 false
     */
    public boolean canPerformHappyInteraction() {
        Pet pet = getOrCreatePet();
        if (pet == null) {
            logger.warning("canPerformHappyInteraction: 无法获取宠物信息");
            return false;
        }
        long lastTime = pet.getLastClickTime();
        long currentTime = System.currentTimeMillis();

        // 检查当前时间是否比 (上次时间 + 冷却) 要晚
        return (currentTime - lastTime >= HAPPY_INTERACTION_COOLDOWN);
    }

    /**
     * 记录一次成功的 "Happy" 互动时间
     * @return 如果更新成功返回 true，否则返回 false
     */
    public boolean recordHappyInteraction() {
        Pet pet = getOrCreatePet();
        if (pet == null) {
            logger.warning("recordHappyInteraction: 无法获取宠物信息");
            return false;
        }
        long currentTime = System.currentTimeMillis();
        pet.setLastClickTime(currentTime);
        return updatePet(pet); // 更新数据库
    }

    /**
     * 获取距离下次可以进行 "Happy" 互动还有多少毫秒
     * @return 剩余的冷却时间（毫秒），如果已经冷却完毕则返回 0
     */
    public long getHappyInteractionRemainingCooldown() {
        Pet pet = getOrCreatePet();
        if (pet == null) {
            logger.warning("getHappyInteractionRemainingCooldown: 无法获取宠物信息");
            return HAPPY_INTERACTION_COOLDOWN; // 无法获取，假设还在冷却
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