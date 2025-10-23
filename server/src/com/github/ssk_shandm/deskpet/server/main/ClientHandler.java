package com.github.ssk_shandm.deskpet.server.main;

import com.github.ssk_shandm.deskpet.server.service.PetService;
import com.github.ssk_shandm.deskpet.server.service.UserService;
// import com.github.ssk_shandm.deskpet.server.model.User;
import com.github.ssk_shandm.deskpet.server.model.Pet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    // private final UserService userService;
    private final PetService petService;

    public ClientHandler(Socket socket, UserService userService, PetService petService) {
        this.clientSocket = socket;
        // this.userService = userService;
        this.petService = petService;
    }

    /**
     * 持续监听与通信
     * inputline:client 输送的请求字符串
     */
    @Override
    public void run() {

        // 获取远程套接字地址
        SocketAddress CA = clientSocket.getRemoteSocketAddress();

        try (
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;

            // 持续监听来自客户端的请求
            while ((inputLine = in.readLine()) != null) {
                System.out.println("收到来自客户端 " + CA + " 的请求: " + inputLine);
                String response = handleRequest(inputLine);
                out.println(response); // 将处理结果发回给客户端
            }
        } catch (IOException e) {
            System.err.println("处理客户端 " + CA + " 时出错: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("客户端 " + CA + " 已断开连接。");
            } catch (IOException e) {
                System.err.println("关闭客户端Socket时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 解析并处理客户端的请求
     */
    private String handleRequest(String request) {
        // 处理请求
        String[] parts = request.split(":");
        String command = parts[0];

        switch (command) {
            case "GET":
                Pet pet = petService.getOrCreatePet();
                return "PET_DATA:" + pet.getName() + "," + pet.getLikeability() + "," + pet.getLastClickTime();

            case "CLICK": // 处理点击事件
                if (petService.canPerformHappyInteraction()) {
                    // 冷却结束，允许互动
                    if (petService.recordHappyInteraction()) {
                        // 成功记录互动时间
                        // 这里可以同时更新好感度等
                        Pet currentPet = petService.getOrCreatePet(); // 获取更新后的宠物状态
                        // 可以选择返回更新后的好感度或其他信息
                        return String.format("CLICK_RESPONSE:SUCCESS,%d,%d", currentPet.getLikeability(),
                                currentPet.getLastClickTime());
                    } else {
                        // 数据库更新失败
                        return "CLICK_RESPONSE:ERROR,数据库更新失败";
                    }
                } else {
                    // 仍在冷却中
                    long remainingCooldown = petService.getHappyInteractionRemainingCooldown();
                    return "CLICK_RESPONSE:COOLDOWN," + remainingCooldown; // 返回剩余冷却毫秒数
                }

            case "UPDATE_LIKEABILITY": // 处理客户端发送的好感度更新请求
                if (parts.length == 2) {
                    try {
                        int changeAmount = Integer.parseInt(parts[1]);
                        Pet currentpet = petService.getOrCreatePet();
                        if (currentpet != null) {
                            int newLikeability = currentpet.getLikeability() + changeAmount;
                            // 添加好感度边界检查 (例如 0-1000)
                            newLikeability = Math.max(0, Math.min(1000, newLikeability));
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

        }

        // // // 登录
        // // case "LOGIN":
        // // if (parts.length == 3) {
        // // User user = userService.login(parts[1], parts[2]);
        // // return user != null ? "LOGIN_SUCCESS:" + user.getUsername() :
        // // "LOGIN_FAILED:用户名或密码错误";
        // // }
        // // return "ERROR:无效的LOGIN命令格式";

        // // // 注册
        // // case "REGISTER":
        // // if (parts.length == 3) {
        // // UserService.RegistrationResult result = userService.register(parts[1],
        // // parts[2]);
        // // return switch (result) {
        // // case SUCCESS -> "REGISTER_SUCCESS:注册成功";
        // // case USERNAME_ALREADY_EXISTS -> "REGISTER_FAILED:用户名已存在";
        // // default -> "REGISTER_FAILED:注册失败";
        // // };
        // // }
        // // return "ERROR:无效的REGISTER命令格式";

        // case "GET":
        // User user = userService.getOrCreateUser();
        // return "USER_INFO:用户名=" + user.getUsername() + ",积分=" + user.getPoints();

        // default:
        return "ERROR:未知的命令";
    }

    // private boolean isClickOnCooldown(long lastClickTime) {
    // if (lastClickTime == 0) {
    // return false;
    // }

    // // 获取当前时间和上次点击的时间
    // ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    // ZonedDateTime lastClickDateTime = ZonedDateTime.ofInstant(
    // java.time.Instant.ofEpochMilli(lastClickTime),
    // ZoneId.systemDefault());

    // // 刷新时间定义
    // ZonedDateTime todayAt4AM = now.with(LocalTime.of(4, 0, 0, 0));

    // ZonedDateTime petDayStart;
    // if (now.isBefore(todayAt4AM)) {
    // petDayStart = todayAt4AM.minusDays(1);
    // } else {
    // petDayStart = todayAt4AM;
    // }

    // return !lastClickDateTime.isBefore(petDayStart);
    // }

}
