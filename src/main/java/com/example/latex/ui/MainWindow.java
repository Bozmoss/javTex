package com.example.latex.ui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

public class MainWindow extends JFrame {
    public MainWindow() {
        setTitle("javTex");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        //JTextArea editor = new JTextArea();
        //editor.setText("Hello Wolrd!");

        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LATEX);
        textArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);

        JPanel preview = new JPanel(new BorderLayout());
        preview.add(new JLabel("PDF Here"), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            sp,
            preview
        );

        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);
    }
}