package com.example.latex.ui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

public class MainWindow extends JFrame {
    public MainWindow() {
        setTitle("javTex");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        JTextArea editor = new JTextArea();
        editor.setText("Hello Wolrd!");

        JPanel preview = new JPanel(new BorderLayout());
        preview.add(new JLabel("PDF Here"), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            editor,
            preview
        );

        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);
    }
}