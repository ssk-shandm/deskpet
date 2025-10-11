package com.github.ssk_shandm.deskpet.server.main;

import com.github.ssk_shandm.deskpet.server.dao.DatabaseUtil;
// import com.github.ssk_shandm.deskpet.server.dao.UserDao;
// import com.github.ssk_shandm.deskpet.server.model.User;
import com.github.ssk_shandm.deskpet.server.service.UserService;

public class Server {
    public static void main(String[] args) {
        // 初始化数据库
        DatabaseUtil.initializeDatabase();
        System.out.println("--- 数据库检查完成 ---");

        // UserDao userDao = new UserDao();

        // 创建用户
        UserService uS = new UserService();
        uS.register("testuser", "123456");

        // 登录用户
        // try {
        //     uS.login("testuser", "123456");
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        // 查询用户
        // System.out.println("尝试查询用户 'testuser'...");
        // User user = userDao.findByUsername("testuser");

        // if (user != null) {
        // System.out.println("成功");
        // }

    }
}