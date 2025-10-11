package com.github.ssk_shandm.deskpet.server.service;

import com.github.ssk_shandm.deskpet.server.dao.UserDao;
import com.github.ssk_shandm.deskpet.server.model.User;

/**
 * UserService (用户业务逻辑服务)
 * 负责处理所有与用户相关的业务规则。
 */
public class UserService {

    private final UserDao userDao = new UserDao();

    /**
     * 定义用户注册的结果枚举，使返回结果更清晰。
     */
    public enum RegistrationResult {
        SUCCESS,
        USERNAME_ALREADY_EXISTS,
        FAILED
    }

    /**
     * 处理用户登录的业务逻辑。
     *
     * @param username 用户名
     * @param password 密码
     * @return 如果登录成功，返回对应的 User 对象；如果失败（用户不存在或密码错误），返回 null。
     */
    public User login(String username, String password) {
        // 验证
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return null;
        }

        // 2. 调用 DAO 从数据库查找用户
        User user = userDao.findByUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            System.out.println("用户 '" + username + "' 登录成功。");
            return user; // 登录成功
        }


        System.out.println("用户 '" + username + "' 登录失败：用户名或密码错误。");
        return null; // 用户不存在或密码错误
    }

    /**
     * 处理用户注册的业务逻辑。
     *
     * @param username 用户名
     * @param password 密码
     * @return 返回一个 RegistrationResult 枚举，表明注册结果。
     */
    public RegistrationResult register(String username, String password) {
        // 验证
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return RegistrationResult.FAILED;
        }

        // 检查用户名是否已经被占用
        if (userDao.findByUsername(username) != null) {
            System.out.println("注册失败：用户名 '" + username + "' 已被占用。");
            return RegistrationResult.USERNAME_ALREADY_EXISTS;
        }

        // 创建新用户对象
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password); // 密码需要加密存储(后期处理)

        // 4. 调用 DAO 将新用户存入数据库
        boolean success = userDao.createUser(newUser);

        if (success) {
            System.out.println("用户 '" + username + "' 注册成功！");
            return RegistrationResult.SUCCESS;
        } else {
            System.out.println("注册失败：写入数据库时发生未知错误。");
            return RegistrationResult.FAILED;
        }
    }
}
