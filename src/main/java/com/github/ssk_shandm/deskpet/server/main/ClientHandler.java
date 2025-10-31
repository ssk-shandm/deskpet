package com.github.ssk_shandm.deskpet.server.main;

import com.github.ssk_shandm.deskpet.server.service.AudioService;
import com.github.ssk_shandm.deskpet.server.service.PetService;
import com.github.ssk_shandm.deskpet.server.service.UserService;
import com.github.ssk_shandm.deskpet.server.model.Pet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
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
    private final AudioService audioService;

    /**
     * 构造函数
     * 
     * @param socket       客户端 Socket
     * @param userService  用户服务实例
     * @param petService   宠物服务实例
     * @param audioService 音频服务实例
     */
    public ClientHandler(Socket socket, UserService userService, PetService petService, AudioService audioService) {
        this.clientSocket = socket;
        // this.userService = userService;
        this.petService = petService;
        this.audioService = audioService;
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
     * 
     * @param request 客户端发送的原始请求字符串
     * @return 响应字符串
     */
    private String handleRequest(String request) {
        String[] parts = request.split(":", 2);
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

            case "GET_DURATIONS":
                // 客户端请求所有缓存的时长
                Map<String, Long> durations = audioService.getDurations(); //
                // Map 编码
                StringBuilder sb = new StringBuilder("DURATIONS_DATA:");
                for (Map.Entry<String, Long> entry : durations.entrySet()) {
                    sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
                }
                return sb.toString();

            case "POST_DURATIONS":
                // 客户端上传新计算的时长
                if (parts.length == 2 && !parts[1].isEmpty()) {
                    try {
                        Map<String, Long> newDurations = new HashMap<>();
                        // 解码 "key1,ms1;key2,ms2" 格式
                        String[] entries = parts[1].split(";");
                        for (String entry : entries) {
                            String[] kv = entry.split(",");
                            if (kv.length == 2) {
                                newDurations.put(kv[0], Long.parseLong(kv[1]));
                            }
                        }
                        if (!newDurations.isEmpty()) {
                            audioService.addDurations(newDurations); //
                            logger.info("成功从客户端保存 " + newDurations.size() + " 条新时长。");
                        }
                        return "POST_DURATIONS_RESPONSE:SUCCESS";
                    } catch (Exception e) {
                        logger.warning("解析 POST_DURATIONS 数据失败: " + e.getMessage());
                        return "POST_DURATIONS_RESPONSE:ERROR,数据格式错误";
                    }
                } else {
                    return "POST_DURATIONS_RESPONSE:ERROR,数据为空";
                }

            default:
                return "ERROR:未知的命令";
        }
    }
}