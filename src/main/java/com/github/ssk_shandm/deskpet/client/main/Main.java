package com.github.ssk_shandm.deskpet.client.main;

import com.formdev.flatlaf.FlatLightLaf;
import com.github.ssk_shandm.deskpet.client.view.PetWindow;
import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 客户端主入口
 * 负责初始化 Look and Feel 并启动 PetWindow。
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * 程序主方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {

        // 在一个新线程中启动服务器
        new Thread(() -> {
            try {
                logger.info("正在启动本地数据库服务(Server)...");
                com.github.ssk_shandm.deskpet.server.main.Server.main(null);
                logger.info("本地数据库服务(Server)已启动。");
            } catch (Exception e) {
                // 如果启动失败，弹窗提示用户
                logger.log(Level.SEVERE, "数据库服务(Server)启动失败！", e);
                JOptionPane.showMessageDialog(null,
                        "数据库服务启动失败！程序无法运行。\n错误: " + e.getMessage(),
                        "严重错误",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1); // 启动失败，退出程序
            }
        }).start();

        // 给后台的 Server 足够的时间完成初始化并监听端口
        try {
            logger.info("等待 Server 初始化... (2秒)");
            Thread.sleep(2000); // 等待 2 秒
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "等待 Server 时被中断", e);
        }


        // 启动客户端 UI
        SwingUtilities.invokeLater(() -> {
            // 设置 Swing 外观 (FlatLaf)
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                logger.info("FlatLaf Look and Feel 设置成功。");
                UIManager.put("ToolTip.showOnDisabledComponents", true);
            } catch (UnsupportedLookAndFeelException e) {
                logger.log(Level.SEVERE, "无法设置 FlatLaf 外观", e);
            }

            // 创建 PetWindow 实例
            logger.info("正在创建 PetWindow 实例 (Client)...");
            new PetWindow();
            logger.info("PetWindow 实例已创建。");
        });
    }
}