package com.example.latex.ui;

import java.awt.BorderLayout;
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
    private boolean isDirty = false;
    private boolean loadingDocument = false;

    private Path currentFile = null;

    private RSyntaxTextArea editor;
    
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

        editor = new RSyntaxTextArea();
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LATEX);
        editor.setCodeFoldingEnabled(true);
        editor.setTabSize(4);
        editor.getDocument().addDocumentListener(
            new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    markDirty();
                }

                @Override 
                public void removeUpdate(DocumentEvent e) {
                    markDirty();
                }

                @Override 
                public void changedUpdate(DocumentEvent e) {
                    markDirty();
                }
            }
        );

        RTextScrollPane sp = new RTextScrollPane(editor);
        sp.setLineNumbersEnabled(true);

        JPanel preview = new JPanel(new BorderLayout());
        preview.add(new JLabel("PDF Here"), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            sp,
            preview
        );

        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open...");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        JMenuItem exitItem = new JMenuItem("Exit");

        newItem.addActionListener(e -> newDocument());
        openItem.addActionListener(e -> openDocument());
        saveItem.addActionListener(e -> saveDocument());
        saveAsItem.addActionListener(e -> saveDocumentAs());
        exitItem.addActionListener(e -> requestClose());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void markDirty() {
        if (!loadingDocument) {
            isDirty = true;
            updateWindowTitle();
        }
    }

    private void updateWindowTitle() {
        String name = (currentFile == null) ? "Untitled" : currentFile.getFileName().toString();

        setTitle("javTex - " + name + (isDirty ? " *" : ""));   
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

            isDirty = false;
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
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Open LaTeX Document");
        chooser.setFileFilter(
            new FileNameExtensionFilter(
                "LaTeX Files (*.tex)", "tex"
            )
        );

        if (chooser.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path selectedFile = chooser.getSelectedFile().toPath();

        // Read first, so a read error doesn't destroy the current document.
        final String content;

        try {
            content = Files.readString(
                selectedFile,
                StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                this,
                "Could not open file:\n" + e.getMessage(),
                "Open Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Only replace the current document if the user agrees.
        if (!confirmUnsavedChanges()) {
            return;
        }

        loadingDocument = true;

        try {
            editor.setText(content);
            editor.setCaretPosition(0);
            currentFile = selectedFile;
            isDirty = false;
        } finally {
            loadingDocument = false;
        }

        updateWindowTitle();
    }

    private void newDocument() {
        if (!confirmUnsavedChanges()) {
            return;
        }

        loadingDocument = true;

        try {
            editor.setText("");
            editor.setCaretPosition(0);
            currentFile = null;
            isDirty = false;
        } finally {
            loadingDocument = false;
        }

        updateWindowTitle();
    }

    private boolean confirmUnsavedChanges() {
        if (!isDirty) {
            return true;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Do you want to save your changes?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            return saveDocument();
        }

        if (choice == JOptionPane.NO_OPTION) {
            return true;
        }

        // Cancel or close the dialog.
        return false;
    }

    private void requestClose() {
        if (confirmUnsavedChanges()) {
            dispose();
        }
    }
}