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
     */
    public String[] getPetData() {
        String response = sendRequest("GET");
        if (response != null && response.startsWith("PET_DATA:")) {
            return response.substring("PET_DATA:".length()).split(",");
        }
        return null;
    }

    /**
     * 请求服务器处理点击
     */
    public String[] sendClick() {
        String response = sendRequest("CLICK");
        if (response != null && response.startsWith("CLICK_RESPONSE:")) {
            return response.substring("CLICK_RESPONSE:".length()).split(",");
        }
        return null;
    }
}