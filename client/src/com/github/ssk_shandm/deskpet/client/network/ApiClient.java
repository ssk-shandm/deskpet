package com.github.ssk_shandm.deskpet.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
     * 
     * @return 好感度和状态组成的字符串数组,格式为 [好感度, 状态];如果获取失败则返回 null 
     */
    public String[] getPetData() {
        String response = sendRequest("GET");
        if (response != null && response.startsWith("PET_DATA:")) {
            // "PET_DATA:100,health" -> "100,health" -> ["100", "health"]
            return response.substring("PET_DATA:".length()).split(",");
        }
        return null;
    }

    /**
     * 请求服务器更新好感度
     */
    public int updateLikeability(int change) {
        String command = "UPDATE:" + change;
        String response = sendRequest(command);
        if (response != null && response.startsWith("UPDATE:")) {
            try {
                return Integer.parseInt(response.substring("UPDATE:".length()));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}