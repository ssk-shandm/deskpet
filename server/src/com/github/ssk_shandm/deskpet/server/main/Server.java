package com.github.ssk_shandm.deskpet.server.main;

import com.github.ssk_shandm.deskpet.server.dao.DatabaseUtil;
import com.github.ssk_shandm.deskpet.server.model.User;
// import com.github.ssk_shandm.deskpet.server.dao.UserDao;
// import com.github.ssk_shandm.deskpet.server.model.User;
import com.github.ssk_shandm.deskpet.server.service.UserService;
import com.github.ssk_shandm.deskpet.server.service.PetService;
// import com.github.ssk_shandm.deskpet.server.model.Pet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 12345;

    public static void main(String[] args) {
        // 初始化数据库
        DatabaseUtil.initializeDatabase();
        System.out.println("--- 数据库检查完成 ---");

        // UserDao userDao = new UserDao();

        // 创建用户
        // UserService uS = new UserService();
        // uS.register("testuser", "123456");

        // 登录用户
        // try {
        // uS.login("testuser", "123456");
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        // 查询用户
        // System.out.println("尝试查询用户 'testuser'...");
        // User user = userDao.findByUsername("testuser");

        // if (user != null) {
        // System.out.println("成功");
        // }

        UserService userService = new UserService();
        PetService petService = new PetService();

        // 获取或创建唯一用户
        User user = userService.getOrCreateUser();
        System.out.println("当前用户: " + user.getUsername() + ", 积分: " + user.getPoints());

        // 获取或创建宠物
        // System.out.println("正在获取宠物信息...");
        // Pet myPet = petService.getOrCreatePet();
        // System.out.println("获取到的宠物名字: " + myPet.getName() + ", 等级: " +
        // myPet.getRank());

        // 更新宠物信息
        // System.out.println("正在更新宠物信息...");
        // myPet.setName("Pikachu");
        // myPet.setRank(myPet.getRank() + 1);
        // boolean success = petService.updatePet(myPet);
        // if (success) {
        // System.out.println("宠物信息更新成功！");
        // } else {
        // System.out.println("宠物信息更新失败。");
        // }

        // Pet updatedPet = petService.getOrCreatePet();
        // System.out.println("更新后的宠物名字: " + updatedPet.getName() + ", 等级: " +
        // updatedPet.getRank());

        // 启动服务器套接字
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("服务器已启动，正在监听端口 " + PORT + " ...");

            // 等待并接受客户端连接
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("接收到新的客户端连接: " + clientSocket.getRemoteSocketAddress());

                // 为每个客户端创建一个新的线程来处理
                ClientHandler clientHandler = new ClientHandler(clientSocket, userService, petService);
                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            System.err.println("服务器运行时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}