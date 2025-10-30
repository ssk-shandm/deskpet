package com.github.ssk_shandm.deskpet.server.main;

import com.github.ssk_shandm.deskpet.server.service.PetService;
import com.github.ssk_shandm.deskpet.server.service.UserService;
import com.github.ssk_shandm.deskpet.server.model.Pet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Logger;

/**
 * 客户端处理器 (Runnable)
 * 负责在单独的线程中处理单个客户端的连接和请求。
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private final Socket clientSocket;
    // private final UserService userService;
    private final PetService petService;

    /**
     * 构造函数
     * @param socket     客户端 Socket
     * @param userService 用户服务实例
     * @param petService  宠物服务实例
     */
    public ClientHandler(Socket socket, UserService userService, PetService petService) {
        this.clientSocket = socket;
        // this.userService = userService;
        this.petService = petService;
    }

    /**
     * 线程执行体：持续监听和响应客户端请求。
     */
    @Override
    public void run() {
        SocketAddress ca = clientSocket.getRemoteSocketAddress(); // 客户端地址

        try (
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;

            // 持续监听来自客户端的请求
            while ((inputLine = in.readLine()) != null) {
                logger.info("收到来自客户端 " + ca + " 的请求: " + inputLine);
                String response = handleRequest(inputLine);
                out.println(response); // 将处理结果发回给客户端
            }
        } catch (IOException e) {
            logger.warning("处理客户端 " + ca + " 时出错: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                logger.info("客户端 " + ca + " 已断开连接。");
            } catch (IOException e) {
                logger.warning("关闭客户端Socket时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 解析并处理客户端的请求
     * @param request 客户端发送的原始请求字符串
     * @return 响应字符串
     */
    private String handleRequest(String request) {
        String[] parts = request.split(":");
        String command = parts[0];

        switch (command) {
            case "GET":
                // 获取宠物数据
                Pet pet = petService.getOrCreatePet();
                return "PET_DATA:" + pet.getName() + "," + pet.getLikeability() + "," + pet.getLastClickTime();

            case "CLICK":
                // 处理点击事件
                if (petService.canPerformHappyInteraction()) {
                    // 冷却结束，允许互动
                    if (petService.recordHappyInteraction()) {
                        Pet currentPet = petService.getOrCreatePet(); // 获取更新后的状态
                        return String.format("CLICK_RESPONSE:SUCCESS,%d,%d",
                                currentPet.getLikeability(), currentPet.getLastClickTime());
                    } else {
                        return "CLICK_RESPONSE:ERROR,数据库更新失败";
                    }
                } else {
                    // 仍在冷却中
                    long remainingCooldown = petService.getHappyInteractionRemainingCooldown();
                    return "CLICK_RESPONSE:COOLDOWN," + remainingCooldown; // 返回剩余毫秒
                }

            case "UPDATE_LIKEABILITY":
                // 处理客户端发送的好感度更新
                if (parts.length == 2) {
                    try {
                        int changeAmount = Integer.parseInt(parts[1]);
                        Pet currentpet = petService.getOrCreatePet();
                        if (currentpet != null) {
                            int newLikeability = currentpet.getLikeability() + changeAmount;
                            // 好感度边界检查 (0-100)
                            newLikeability = Math.max(0, Math.min(100, newLikeability));
                            currentpet.setLikeability(newLikeability);
                            
                            if (petService.updatePet(currentpet)) {
                                return "UPDATE_LIKEABILITY_RESPONSE:SUCCESS," + newLikeability;
                            } else {
                                return "UPDATE_LIKEABILITY_RESPONSE:ERROR,更新失败";
                            }
                        } else {
                            return "UPDATE_LIKEABILITY_RESPONSE:ERROR,找不到宠物";
                        }
                    } catch (NumberFormatException e) {
                        return "ERROR:无效的好感度值";
                    }
                } else {
                    return "ERROR:无效的UPDATE_LIKEABILITY命令格式";
                }

            default:
                return "ERROR:未知的命令";
        }
    }
}