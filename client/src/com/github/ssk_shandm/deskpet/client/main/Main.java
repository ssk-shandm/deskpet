package com.github.ssk_shandm.deskpet.client.main;

import com.formdev.flatlaf.FlatLightLaf; 
import com.github.ssk_shandm.deskpet.client.view.PetWindow;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {

        FlatLightLaf.setup(); 

        SwingUtilities.invokeLater(() -> {
            PetWindow petWindow = new PetWindow();
            petWindow.showPet();
        });
    }
}