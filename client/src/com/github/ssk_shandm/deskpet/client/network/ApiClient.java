package com.github.ssk_shandm.deskpet.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * API 客户端
 * 负责处理所有与服务器的 Socket 通信。
 */
public class ApiClient {

    private static final Logger logger = Logger.getLogger(ApiClient.class.getName());
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    /**
     * 从服务器获取初始宠物数据
     * @return 包含 "name", "likeability", "lastClickTime" 的 Map, 失败返回 null 或空 Map
     */
    public Map<String, String> getPetData() {
        String response = sendRequest("GET");
        Map<String, String> petInfo = new HashMap<>();

        if (response == null || response.startsWith("ERROR:")) {
            logger.warning("getPetData 失败: " + response);
            return petInfo; // 返回空 Map
        }

        if (response.startsWith("PET_DATA:")) {
            try {
                // PET_DATA:name,likeability,lastClickTime
                String[] data = response.substring("PET_DATA:".length()).split(",");
                if (data.length >= 3) {
                    petInfo.put("name", data[0]);
                    petInfo.put("likeability", data[1]);
                    petInfo.put("lastClickTime", data[2]);
                    return petInfo;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "解析宠物数据时出错: " + response, e);
            }
        }
        return petInfo; // 解析失败也返回空 Map
    }

    /**
     * 请求服务器处理一次点击
     * @return 包含响应状态 ("status") 和其他数据 (如 "likeability", "remainingTime") 的 Map
     */
    public Map<String, String> sendClick() {
        String response = sendRequest("CLICK");
        Map<String, String> result = new HashMap<>();

        if (response == null || response.startsWith("ERROR:")) {
            result.put("status", "ERROR");
            result.put("message", response != null ? response.substring("ERROR:".length()) : "无法连接到服务器");
            return result;
        }

        if (response.startsWith("CLICK_RESPONSE:")) {
            // CLICK_RESPONSE:STATUS,data1,data2...
            String[] parts = response.substring("CLICK_RESPONSE:".length()).split(",");
            result.put("status", parts[0]); // SUCCESS, COOLDOWN, ERROR

            switch (parts[0]) {
                case "SUCCESS": // SUCCESS,likeability,lastClickTime
                    if (parts.length >= 3) {
                        result.put("likeability", parts[1]);
                        result.put("lastClickTime", parts[2]);
                    }
                    break;
                case "COOLDOWN": // COOLDOWN,remainingTime
                    if (parts.length >= 2) {
                        result.put("remainingTime", parts[1]);
                    }
                    break;
                case "ERROR": // ERROR,message
                    if (parts.length >= 2) {
                        result.put("message", parts[1]);
                    }
                    break;
            }
        } else {
            result.put("status", "ERROR");
            result.put("message", "收到服务器未知响应: " + response);
        }
        return result;
    }

    /**
     * 向服务器发送好感度变化请求
     * @param changeAmount 好感度变化量 (正数或负数)
     * @return 包含响应状态 ("status") 和新好感度 ("newLikeability") 的 Map
     */
    public Map<String, String> updateLikeability(int changeAmount) {
        String request = "UPDATE_LIKEABILITY:" + changeAmount;
        String response = sendRequest(request);
        Map<String, String> result = new HashMap<>();

        if (response == null || response.startsWith("ERROR:")) {
            result.put("status", "ERROR");
            result.put("message", response != null ? response.substring("ERROR:".length()) : "无法连接到服务器");
            return result;
        }

        if (response.startsWith("UPDATE_LIKEABILITY_RESPONSE:")) {
            // UPDATE_LIKEABILITY_RESPONSE:STATUS,newData
            String[] parts = response.substring("UPDATE_LIKEABILITY_RESPONSE:".length()).split(",");
            result.put("status", parts[0]); // SUCCESS or ERROR

            if ("SUCCESS".equals(parts[0]) && parts.length >= 2) {
                result.put("newLikeability", parts[1]);
            } else if ("ERROR".equals(parts[0]) && parts.length >= 2) {
                result.put("message", parts[1]);
            }
        } else {
            result.put("status", "ERROR");
            result.put("message", "收到服务器未知响应: " + response);
        }
        return result;
    }

    /**
     * (私有) 向服务器发送一个请求字符串，并接收返回的响应字符串
     * @param request 发送给服务器的请求 (例如 "GET" 或 "CLICK")
     * @return 服务器的响应, 或 "ERROR:..."
     */
    private String sendRequest(String request) {
        // 使用 try-with-resources 自动关闭 Socket 和流
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 发送请求
            logger.info("C -> S: " + request);
            out.println(request);

            // 接收响应
            String response = in.readLine();
            logger.info("S -> C: " + response);
            return response;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "连接服务器时出错 (" + request + "): " + e.getMessage(), e);
            return "ERROR:无法连接到服务器";
        }
    }
}