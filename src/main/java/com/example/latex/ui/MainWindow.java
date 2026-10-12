package com.example.latex.ui;

import com.example.latex.compiler.Compiler;
import com.example.latex.editor.Editor;
import com.example.latex.pdf.PDFViewer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

public class MainWindow extends JFrame {
    private final Compiler latexCompiler = new Compiler();

    private final Editor editor = new Editor(this::updateWindowTitle);

    private final JTextArea buildLog = new JTextArea(8, 80);

    private Path currentFile = null;

    private final PDFViewer pdfViewer = new PDFViewer();
    
    public MainWindow() {
        createWindow();
    }

    public void createWindow() {
        setTitle("javTex");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override 
            public void windowClosing(WindowEvent e) {
                requestClose();
            }
        });

        setSize(1200, 800);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            editor.getScrollPane(),
            pdfViewer
        );

        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        buildLog.setEditable(false);
        buildLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        buildLog.setText("Ready to Compile.");

        add(new JScrollPane(buildLog), BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open...");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        JMenuItem exitItem = new JMenuItem("Exit");
        JMenuItem compileItem = new JMenuItem("Compile");

        newItem.addActionListener(e -> newDocument());
        openItem.addActionListener(e -> openDocument());
        saveItem.addActionListener(e -> saveDocument());
        saveAsItem.addActionListener(e -> saveDocumentAs());
        exitItem.addActionListener(e -> requestClose());
        compileItem.addActionListener(e -> compileDocument());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(exitItem);
        fileMenu.addSeparator();
        fileMenu.add(compileItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void updateWindowTitle() {
        String name = (currentFile == null) ? "Untitled" : currentFile.getFileName().toString();

        setTitle("javTex - " + name + (editor.isDirty() ? " *" : ""));   
    }

    private boolean saveDocument() {
        if (currentFile == null) {
            return saveDocumentAs();
        }
        
        try {
            Files.writeString(
                currentFile,
                editor.getText(),
                StandardCharsets.UTF_8
            );

            editor.setDirty(false);
            updateWindowTitle();
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                this,
                "Could not save file:\n" + e.getMessage(),
                "Save Error",
                JOptionPane.ERROR_MESSAGE
            );

            return false;
        }
    }

    private boolean saveDocumentAs() {
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Save LaTeX document");
        chooser.setFileFilter(
            new FileNameExtensionFilter(
                "Latex Files (*.tex)", "tex"
            )
        );

        if (currentFile != null) {
            chooser.setSelectedFile(currentFile.toFile());
        }
        else {
            chooser.setSelectedFile(
                new java.io.File("Untitled.tex")
            );
        }

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        Path selectedFile = chooser.getSelectedFile().toPath();

        if (!selectedFile.getFileName().toString().contains(".")) {
            selectedFile = selectedFile.resolveSibling(selectedFile.getFileName() + ".tex");
        }

        if (Files.exists(selectedFile) && !selectedFile.equals(currentFile)) {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "This file already exists. Overwrite it?",
                "Confirm Overwrite",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        currentFile = selectedFile;
        return saveDocument();
    }

    private void openDocument() {
        currentFile = editor.openDocument(currentFile, this::saveDocument);
    }

    private void newDocument() {
        currentFile = editor.newDocument(currentFile, this::saveDocument);
    }

    private void requestClose() {
        if (editor.confirmUnsavedChanges(this::saveDocument)) {
            dispose();
        }
    }

    private void compileDocument() {
        if (currentFile == null) {
            if (!saveDocumentAs()) {
                return;
            }
        }
        else if (editor.isDirty()) {
            if (!saveDocument()) {
                return;
            }
        }

        Path fileToCompile = currentFile;

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