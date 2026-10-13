package com.example.latex.editor;

import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BooleanSupplier;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;

import org.fife.ui.rsyntaxtextarea.CodeTemplateManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class Editor extends RSyntaxTextArea {
    private boolean isDirty = false;
    private boolean loadingDocument = false;

    private final Runnable updateWindowTitle;

    private final RTextScrollPane scrollPane;

    private ShortcutManager shortcutManager;

    public Editor(Runnable updateWindowTitle) {
        this.updateWindowTitle = updateWindowTitle;
        setTemplatesEnabled(true);

        Path defaultPath = Paths.get("target/classes/default.hotkey");
        CodeTemplateManager ctm = getCodeTemplateManager();
        try {
            shortcutManager = new ShortcutManager(defaultPath, ctm);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                this,
                "Could not open hotkey file:\n" + e.getMessage(),
                "Hotkey error",
                JOptionPane.ERROR_MESSAGE
            );
        }

        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LATEX);
        setCodeFoldingEnabled(true);
        setTabSize(4);
        getDocument().addDocumentListener(
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

        scrollPane = new RTextScrollPane(this);
        scrollPane.setLineNumbersEnabled(true);
    }

    public Path openDocument(Path currentFile, BooleanSupplier saveDocument) {
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Open LaTeX Document");
        chooser.setFileFilter(
            new FileNameExtensionFilter(
                "LaTeX Files (*.tex)", "tex"
            )
        );

        if (chooser.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return currentFile;
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
            return currentFile;
        }

        // Only replace the current document if the user agrees.
        if (!confirmUnsavedChanges(saveDocument)) {
            return currentFile;
        }

        loadingDocument = true;

        try {
            setText(content);
            setCaretPosition(0);
            currentFile = selectedFile;
            isDirty = false;
        } finally {
            loadingDocument = false;
        }

        return currentFile;
    }

    public Path newDocument(Path currentFile, BooleanSupplier saveDocument) {
        if (!confirmUnsavedChanges(saveDocument)) {
            return currentFile;
        }

        loadingDocument = true;

        try {
            setText("");
            setCaretPosition(0);
            currentFile = null;
            isDirty = false;
        } finally {
            loadingDocument = false;
        }

        return currentFile;
    }

    public boolean confirmUnsavedChanges(BooleanSupplier saveDocument) {
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
            return saveDocument.getAsBoolean();
        }

        if (choice == JOptionPane.NO_OPTION) {
            return true;
        }

        // Cancel or close the dialog.
        return false;
    }

    public RTextScrollPane getScrollPane() {
        return scrollPane;
    }

    private void markDirty() {
        if (!loadingDocument) {
            isDirty = true;
            updateWindowTitle.run();
        }
    }

    public boolean isDirty() {
        return isDirty;
    }
    
    public void setDirty(boolean b) {
        isDirty = b;
    }

    public void setLoadingDocument(boolean b) {
        loadingDocument = b;
    }

    public boolean isLoadingDocument() {
        return loadingDocument;
    }
}
