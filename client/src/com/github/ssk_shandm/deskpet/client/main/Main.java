package com.github.ssk_shandm.deskpet.client.main;

import com.formdev.flatlaf.FlatLightLaf;
import com.github.ssk_shandm.deskpet.client.network.ApiClient;
import com.github.ssk_shandm.deskpet.client.view.PetWindow;

import javax.swing.*;
import java.util.concurrent.ExecutionException;

public class   Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();

        // 启动时，先异步获取宠物数据，成功后再创建窗口
        SwingWorker<String[], Void> dataFetcher = new SwingWorker<>() {
            @Override
            protected String[] doInBackground() throws Exception {
                ApiClient apiClient = new ApiClient();
                // 假设总是加载 "seia"，或者从配置/登录逻辑中获取
                // 这里直接调用获取数据的方法
                return apiClient.getPetData();
            }

            @Override
            protected void done() {
                try {
                    String[] petData = get();
                    if (petData != null && petData.length == 3) {
                        String petName = petData[0];
                        int likeability = Integer.parseInt(petData[1]);
                        String status = petData[2];

                        System.out.println("成功获取初始数据: Name=" + petName + ", Likeability=" + likeability + ", Status=" + status);

                        // 在 EDT 上创建 PetWindow
                        SwingUtilities.invokeLater(() -> {
                            new PetWindow(petName, likeability, status);
                        });

                    } else {
                        System.err.println("启动失败：无法从服务器获取有效的宠物数据。");
                        // 可以在这里显示一个错误对话框
                        JOptionPane.showMessageDialog(null, "无法连接到服务器或获取宠物数据。", "启动错误", JOptionPane.ERROR_MESSAGE);
                        System.exit(1); // 退出程序
                    }
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("获取宠物数据时发生异常: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "启动时发生网络错误。", "启动错误", JOptionPane.ERROR_MESSAGE);
                    System.exit(1); // 退出程序
                } catch (NumberFormatException e) {
                    System.err.println("获取的好感度数据格式不正确: " + e.getMessage());
                    JOptionPane.showMessageDialog(null, "接收到的宠物数据格式不正确。", "启动错误", JOptionPane.ERROR_MESSAGE);
                    System.exit(1); // 退出程序
                }
            }
        };

        dataFetcher.execute();
    }
}