package com.github.ssk_shandm.deskpet.server.service;

import com.github.ssk_shandm.deskpet.server.dao.UserDao;
import com.github.ssk_shandm.deskpet.server.model.User;
import java.util.logging.Logger;

/**
 * 用户服务
 * 负责处理与用户相关的业务逻辑。
 */
public class UserService {

    private final UserDao userDao = new UserDao();
    private static final Logger logger = Logger.getLogger(UserService.class.getName());

    /**
     * 获取或创建唯一用户 (固定 ID=1)
     * @return User 对象
     */
    public User getOrCreateUser() {
        User user = userDao.findUser(1);
        if (user == null) {
            logger.info("未找到用户 (ID=1)，正在创建默认用户...");
            User newUser = new User();
            newUser.setUsername("DeskPetUser"); // 默认用户名
            userDao.createUser(newUser);
            user = userDao.findUser(1); // 重新获取
            logger.info("默认用户创建成功！");
        }
        return user;
    }
}