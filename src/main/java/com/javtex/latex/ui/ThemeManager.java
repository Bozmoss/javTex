package com.javtex.latex.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;

import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.Theme;

import com.javtex.latex.editor.Editor;

public class ThemeManager {
    private ThemeManager() {}

    public static void applyTheme(JFrame window, Editor editor, boolean dark) {
        try {
            UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
            SwingUtilities.updateComponentTreeUI(window);

            String resource = dark ? "/org/fife/ui/rsyntaxtextarea/themes/dark.xml" : "/org/fife/ui/rsyntaxtextarea/themes/default.xml";

            try (var input = ThemeManager.class.getResourceAsStream(resource)) {
                if (input == null) {
                    throw new IOException("Could not find editor theme: " + resource);
                }
                Theme theme = Theme.load(input);
                theme.apply(editor);
            }

            window.repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                window,
                "Could not change theme:\n" + e.getMessage(),
                "Theme error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
