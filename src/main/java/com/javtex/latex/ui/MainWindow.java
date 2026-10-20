package com.javtex.latex.ui;

import com.javtex.latex.compiler.Compiler;
import com.javtex.latex.editor.Editor;
import com.javtex.latex.pdf.PDFViewer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

import javax.swing.*;

public class MainWindow extends JFrame {
    private final Compiler latexCompiler = new Compiler();

    private final Editor editor = new Editor(this::updateWindowTitle);
    private final Menu menu;

    private final JTextArea buildLog = new JTextArea(8, 80);

    private final PDFViewer pdfViewer = new PDFViewer();

    public MainWindow() {
        menu = new Menu(this, editor, this::compileDocument);
        createWindow();
    }

    public void createWindow() {
        setTitle("javTex");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override 
            public void windowClosing(WindowEvent e) {
                menu.requestClose();
            }
        });

        setSize(1200, 800);
        setLocationRelativeTo(null);
        menu.applyInitialTheme();

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            editor.getScrollPane(),
            pdfViewer
        );

        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));

        buildLog.setEditable(false);
        buildLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        buildLog.setText("Ready to Compile.");

        add(new JScrollPane(buildLog), BorderLayout.SOUTH);

        setJMenuBar(menu);
    }

    void updateWindowTitle() {
        Path currentFile = menu.getCurrentFile();
        String name = (currentFile == null) ? "Untitled" : currentFile.getFileName().toString();

        setTitle("javTex - " + name + (editor.isDirty() ? " *" : ""));
    }

    private void compileDocument() {
        if (menu.getCurrentFile() == null) {
            if (!menu.saveDocumentAs()) {
                return;
            }
        }
        else if (editor.isDirty()) {
            if (!menu.saveDocument()) {
                return;
            }
        }

        Path fileToCompile = menu.getCurrentFile();

        buildLog.setText("Compiling " + fileToCompile.getFileName() + "...\n");

        SwingWorker<Compiler.CompilationResult, Void> worker = new SwingWorker<>() {
            @Override 
            protected Compiler.CompilationResult doInBackground() throws Exception {
                return latexCompiler.compile(fileToCompile);
            }

            @Override 
            protected void done() {
                try {
                    Compiler.CompilationResult result = get();

                    buildLog.append(result.output());
                    buildLog.append("\n\nExit code: " + result.exitCode());

                    if (result.isSuccess()) {
                        buildLog.append("\nCompilation succeeded.");

                        String texName = fileToCompile.getFileName().toString();

                        String pdfName = texName.replaceFirst("(?i)\\.tex$", ".pdf");

                        Path pdfFile = fileToCompile.resolveSibling(pdfName);

                        pdfViewer.loadPDF(pdfFile);
                    }
                    else {
                        buildLog.append("\nCompilation failed.");
                    }
                } catch (Exception ex) {
                    buildLog.append("\nCould not run LaTeX:\n" + ex.getMessage());
                }

                buildLog.setCaretPosition(buildLog.getDocument().getLength());
            }
        };

        worker.execute();
    }
}