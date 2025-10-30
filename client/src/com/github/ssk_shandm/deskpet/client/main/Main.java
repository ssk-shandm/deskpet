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
     * @param args 命令行参数 (未使用)
     */
    public static void main(String[] args) {
        // 确保所有 GUI 操作都在事件调度线程 (EDT) 中执行
        SwingUtilities.invokeLater(() -> {
            
            // 设置 Swing 外观 (FlatLaf)
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                logger.info("FlatLaf Look and Feel 设置成功。");

                // 允许在禁用的组件上显示工具提示 (ToolTip)
                UIManager.put("ToolTip.showOnDisabledComponents", true);

            } catch (UnsupportedLookAndFeelException e) {
                logger.log(Level.SEVERE, "无法设置 FlatLaf 外观", e);
            }

            // 创建 PetWindow 实例
            // PetWindow 的构造函数将处理后续所有加载
            logger.info("正在创建 PetWindow 实例...");
            new PetWindow();
            logger.info("PetWindow 实例已创建。");
        });
    }
}