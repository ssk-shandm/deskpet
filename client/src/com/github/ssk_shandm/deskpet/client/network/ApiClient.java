package com.github.ssk_shandm.deskpet.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * 负责处理所有与服务器的通信
 */
public class ApiClient {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    /**
     * 向服务器发送一个请求，并接收返回的响应
     */
    private String sendRequest(String request) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                // 输出流和输入流
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 请求
            System.out.println("C -> S: " + request);
            out.println(request);

            // 响应
            String response = in.readLine();
            System.out.println("S -> C: " + response);
            return response;

        } catch (IOException e) {
            System.err.println("连接服务器时出错: " + e.getMessage());
            return "ERROR:无法连接到服务器";
        }
    }

    /**
     * 从服务器获取宠物数据
     */
    public Map<String, String> getPetData() {
        String response = sendRequest("GET");
        if (response != null && response.startsWith("PET_DATA:")) {
            try {
                String[] data = response.substring("PET_DATA:".length()).split(",");
                if (data.length >= 3) { 
                    Map<String, String> petInfo = new HashMap<>();
                    petInfo.put("name", data[0]);
                    petInfo.put("likeability", data[1]);
                    petInfo.put("lastClickTime", data[2]);
                    return petInfo;
                }
            } catch (Exception e) {
                System.err.println("解析宠物数据时出错: " + response);
            }
        }
        return null;
    }

    /**
     * 请求服务器处理点击
     */
    public Map<String, String> sendClick() {
        String response = sendRequest("CLICK");
        Map<String, String> result = new HashMap<>();

        if (response == null || response.equals("ERROR:CONNECTION_FAILED")) {
            result.put("status", "ERROR");
            result.put("message", "无法连接到服务器");
            return result;
        }

        if (response.startsWith("CLICK_RESPONSE:")) {
            String[] parts = response.substring("CLICK_RESPONSE:".length()).split(",");
            result.put("status", parts[0]); // SUCCESS, COOLDOWN, ERROR

            if ("SUCCESS".equals(parts[0]) && parts.length >= 3) {
                result.put("likeability", parts[1]);
                result.put("lastClickTime", parts[2]);
            } else if ("COOLDOWN".equals(parts[0]) && parts.length >= 2) {
                result.put("remainingTime", parts[1]);
            } else if ("ERROR".equals(parts[0]) && parts.length >= 2) {
                result.put("message", parts[1]);
            }
        } else if (response.startsWith("ERROR:")) {
            result.put("status", "ERROR");
            result.put("message", response.substring("ERROR:".length()));
        } else {
            result.put("status", "ERROR");
            result.put("message", "收到服务器未知响应: " + response);
        }
        return result;
    }

    /**
     * 向服务器发送好感度变化请求
     */
    public Map<String, String> updateLikeability(int changeAmount) {
        String request = "UPDATE_LIKEABILITY:" + changeAmount;
        String response = sendRequest(request);
        Map<String, String> result = new HashMap<>();

        if (response == null || response.equals("ERROR:CONNECTION_FAILED")) {
            result.put("status", "ERROR");
            result.put("message", "无法连接到服务器");
            return result;
        }

        if (response.startsWith("UPDATE_LIKEABILITY_RESPONSE:")) {
            String[] parts = response.substring("UPDATE_LIKEABILITY_RESPONSE:".length()).split(",");
            result.put("status", parts[0]); // SUCCESS or ERROR
            if ("SUCCESS".equals(parts[0]) && parts.length >= 2) {
                result.put("newLikeability", parts[1]);
            } else if ("ERROR".equals(parts[0]) && parts.length >= 2) {
                result.put("message", parts[1]);
            }
        } else if (response.startsWith("ERROR:")) {
            result.put("status", "ERROR");
            result.put("message", response.substring("ERROR:".length()));
        } else {
            result.put("status", "ERROR");
            result.put("message", "收到服务器未知响应: " + response);
        }
        return result;
    }
}