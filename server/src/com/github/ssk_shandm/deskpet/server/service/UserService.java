package com.github.ssk_shandm.deskpet.server.service;

import com.github.ssk_shandm.deskpet.server.dao.UserDao;
import com.github.ssk_shandm.deskpet.server.model.User;

/**
 * 用户行为
 */
public class UserService {

    private final UserDao userDao = new UserDao();

    /**
     * 结果枚举
     */
    // public enum RegistrationResult {
    // SUCCESS,
    // USERNAME_ALREADY_EXISTS,
    // FAILED
    // }

    /**
     * 登录方法
     *
     * @param username 用户名
     * @param password 密码
     */
    // public User login(String username, String password) {
    // public User enter(int id) {
    // // 验证
    // // if (username == null || username.isEmpty() || password == null ||
    // // password.isEmpty()) {
    // // return null;
    // }

    // // 调用 DAO 从数据库查找用户
    // User user = userDao.findByUsername(username);

    // if (user != null && user.getPassword().equals(password)) {
    // System.out.println("用户 '" + username + "' 登录成功。");
    // return user; // 登录成功
    // }

    // System.out.println("用户 '" + username + "' 登录失败：用户名或密码错误。");
    // return null; // 用户不存在或密码错误
    // }

    /**
     * 注册方法
     *
     * @param username 用户名
     * @param password 密码
     */
    // public RegistrationResult register(String username, String password) {
    // // 验证
    // if (username == null || username.isEmpty() || password == null ||
    // password.isEmpty()) {
    // return RegistrationResult.FAILED;
    // }

    // // 检查用户名是否已经被占用
    // if (userDao.findByUsername(username) != null) {
    // System.out.println("注册失败：用户名 '" + username + "' 已被占用。");
    // return RegistrationResult.USERNAME_ALREADY_EXISTS;
    // }

    // // 创建新用户对象
    // User newUser = new User();
    // newUser.setUsername(username);
    // newUser.setPassword(password); // 密码需要加密存储(后期处理)

    // // 存入数据库
    // boolean success = userDao.createUser(newUser);

    // if (success) {
    // System.out.println("用户 '" + username + "' 注册成功！");
    // return RegistrationResult.SUCCESS;
    // } else {
    // System.out.println("注册失败：写入数据库时发生未知错误。");
    // return RegistrationResult.FAILED;
    // }
    // }

    /**
     * 获取或创建唯一用户
     * * @return User 对象
     */
    public User getOrCreateUser() {
        User user = userDao.findUser(1);
        if (user == null) {
            System.out.println("未找到用户，正在创建默认用户...");
            User newUser = new User();
            newUser.setUsername("DeskPetUser");
            userDao.createUser(newUser);
            user = userDao.findUser(1);
            System.out.println("默认用户创建成功！");
        }
        return user;
    }
}
