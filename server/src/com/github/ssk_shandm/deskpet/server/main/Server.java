package com.github.ssk_shandm.deskpet.server.main;

import com.github.ssk_shandm.deskpet.server.dao.DatabaseUtil;
import com.github.ssk_shandm.deskpet.server.service.UserService;
import com.github.ssk_shandm.deskpet.server.service.PetService;
import com.github.ssk_shandm.deskpet.server.service.AudioService; // [!! 新增导入 !!]

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 服务器主入口
 * 负责初始化数据库和监听 Socket 连接。
 */
public class Server {

    private static final int PORT = 12345;
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    /**
     * 服务器主方法
     * 
     * @param args 命令行参数 (未使用)
     */
    public static void main(String[] args) {

        // 初始化数据库 (检查或创建 .db 文件和表)
        DatabaseUtil.initializeDatabase(); //
        logger.info("--- 数据库检查完成 ---");

        // 实例化服务
        UserService userService = new UserService(); 
        PetService petService = new PetService(); 
        AudioService audioService = new AudioService(); 

        // 启动服务器套接字
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("服务器已启动，正在监听端口 " + PORT + " ...");

            // 持续等待并接受客户端连接
            while (true) {
                Socket clientSocket = serverSocket.accept(); // 阻塞，直到有连接
                logger.info("接收到新的客户端连接: " + clientSocket.getRemoteSocketAddress());

                // 为新客户端创建一个线程来处理
                ClientHandler clientHandler = new ClientHandler(clientSocket, userService, petService, audioService); //
                new Thread(clientHandler, "Client-" + clientSocket.getPort()).start();
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "服务器运行时出错: " + e.getMessage(), e);
        }
    }
}