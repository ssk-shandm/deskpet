package com.github.ssk_shandm.deskpet.client.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 客户端 API
 * 负责通过 Socket 与服务器通信。
 */
public class ApiClient {

    private static final Logger logger = Logger.getLogger(ApiClient.class.getName());
    private final String host = "localhost";
    private final int port = 12345;

    public ApiClient() {
        // 构造函数
    }

    /**
     * 发送一个请求并获取一行响应
     * @param request 要发送的命令 (例如 "GET" 或 "CLICK")
     * @return 服务器的响应字符串
     */
    private String sendRequest(String request) {
        // 为每个请求打开一个新 Socket (效率低，但与你现有模式匹配)
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())))
        {
            out.println(request); // 发送请求
            String response = in.readLine(); // 等待响应
            if (response == null) {
                return "ERROR:服务器未响应";
            }
            return response;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "ApiClient 请求失败: " + request, e);
            return "ERROR:连接服务器失败," + e.getMessage();
        }
    }

    /**
     * 获取宠物数据
     */
    public Map<String, String> getPetData() {
        String response = sendRequest("GET"); 
        // PET_DATA:seia,100,0
        return parseResponse(response, "PET_DATA", new String[]{"name", "likeability", "lastClickTime"});
    }

    /**
     * 发送点击
     */
    public Map<String, String> sendClick() {
        String response = sendRequest("CLICK"); 
        return parseResponse(response, "CLICK_RESPONSE", new String[]{"status", "likeability", "lastClickTime"});
    }

    /**
     * 更新好感度
     */
    public Map<String, String> updateLikeability(int changeAmount) {
        String response = sendRequest("UPDATE_LIKEABILITY:" + changeAmount); 
        return parseResponse(response, "UPDATE_LIKEABILITY_RESPONSE", new String[]{"status", "newLikeability"});
    }
    
    /**
     * 从服务器获取所有已缓存的音频时长
     * @return Map<String, Long>
     */
    public Map<String, Long> getAudioDurations() {
        logger.info("正在从服务器获取音频时长缓存...");
        String response = sendRequest("GET_DURATIONS"); 
        
        // 响应格式: DURATIONS_DATA:key1,ms1;key2,ms2;
        if (response.startsWith("DURATIONS_DATA:")) {
            Map<String, Long> durations = new HashMap<>();
            try {
                String data = response.substring("DURATIONS_DATA:".length());
                if (data.isEmpty()) {
                    return Collections.emptyMap(); // 数据库为空
                }
                String[] entries = data.split(";");
                for (String entry : entries) {
                    String[] kv = entry.split(",");
                    if (kv.length == 2) {
                        durations.put(kv[0], Long.parseLong(kv[1]));
                    }
                }
                logger.info("成功获取 " + durations.size() + " 条时长缓存。");
                return durations;
            } catch (Exception e) {
                 logger.log(Level.WARNING, "解析 GET_DURATIONS 响应失败", e);
                 return Collections.emptyMap();
            }
        }
        logger.warning("获取音频时长失败: " + response);
        return Collections.emptyMap(); // 失败时返回空 Map
    }
    
    /**
     * [!! 新增 !!]
     * 将新计算的时长 Map 异步上传到服务器
     * @param durationsMap
     */
    public void saveAudioDurationsAsync(Map<String, Long> durationsMap) {
        if (durationsMap == null || durationsMap.isEmpty()) {
            return;
        }
        
        // 将 Map 编码为 "key1,ms1;key2,ms2"
        StringBuilder sb = new StringBuilder("POST_DURATIONS:"); //
        for (Map.Entry<String, Long> entry : durationsMap.entrySet()) {
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
        }
        
        String request = sb.toString();

        // 异步发送，不阻塞主线程
        new Thread(() -> {
            logger.info("正在后台向服务器上传 " + durationsMap.size() + " 条新时长...");
            String response = sendRequest(request);
            if (response.startsWith("POST_DURATIONS_RESPONSE:SUCCESS")) { //
                logger.info("时长上传成功。");
            } else {
                logger.warning("时长上传失败: " + response);
            }
        }, "Audio-Uploader-Thread").start();
    }

    /**
     * 辅助方法：解析服务器响应
     */
    private Map<String, String> parseResponse(String response, String expectedHeader, String[] fieldNames) {
        Map<String, String> map = new HashMap<>();
        if (response.startsWith(expectedHeader + ":")) {
            String[] data = response.substring(expectedHeader.length() + 1).split(",");
            for (int i = 0; i < data.length && i < fieldNames.length; i++) {
                map.put(fieldNames[i], data[i]);
            }
            // 自动设置 status (基于 header)
            if (data.length > 0 && fieldNames[0].equals("status")) {
                 map.put("status", data[0]);
            } else {
                 map.put("status", "SUCCESS"); // 默认
            }
        } else if (response.startsWith("ERROR:")) {
            map.put("status", "ERROR");
            map.put("message", response.substring("ERROR:".length()));
        } else {
             map.put("status", "ERROR");
             map.put("message", "未知响应: " + response);
        }
        return map;
    }
}