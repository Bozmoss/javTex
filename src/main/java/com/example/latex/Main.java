package com.example.latex;

import com.example.latex.ui.MainWindow;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;

import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);

        boolean dark = prefs.getBoolean("darkMode", false);

        if (dark) {
            FlatDarkLaf.setup();
        }
        else {
            FlatLightLaf.setup();
        }

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}