package com.github.ssk_shandm.deskpet.client.main;

import com.github.ssk_shandm.deskpet.client.view.PetWindow;

public class Main {

    public static void main(String[] args) {
        System.out.println("桌面宠物客户端正在启动...");

        // 创建宠物窗口实例
        PetWindow petWindow = new PetWindow();
        
        // 显示宠物
        petWindow.showPet();

        System.out.println("宠物动画已启动！");
    }
}