// client/src/com/github/ssk_shandm/deskpet/client/main/Main.java
package com.github.ssk_shandm.deskpet.client.main;

import com.formdev.flatlaf.FlatLightLaf;
import com.github.ssk_shandm.deskpet.client.view.PetWindow;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 将 GUI 相关操作放入 EDT
        SwingUtilities.invokeLater(() -> {
            // 设置 Swing 外观
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                logger.info("FlatLaf Look and Feel 设置成功。");
            } catch (UnsupportedLookAndFeelException e) {
                logger.log(Level.SEVERE, "无法设置 FlatLaf 外观", e);
                // 可以选择显示警告，但通常程序仍能使用默认外观运行
                // JOptionPane.showMessageDialog(null, "无法加载外观样式，将使用默认外观。", "外观警告", JOptionPane.WARNING_MESSAGE);
            }

            // 直接创建 PetWindow 实例
            // PetWindow 的构造函数内部会负责异步加载所需的数据
            logger.info("正在创建 PetWindow 实例...");
            new PetWindow();
            logger.info("PetWindow 实例已创建。");
        });
    }

    // showErrorAndExit 方法不再需要，因为 PetWindow 自己处理错误
}