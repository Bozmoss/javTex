package com.javtex.latex.ui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.javtex.latex.editor.Editor;

public class Menu extends JMenuBar {
    private final MainWindow owner;
    private final Editor editor;
    private final Preferences preferences;
    private final Runnable compileAction;

    private Path currentFile;
    private boolean darkMode;
    private boolean macrosEnabled;

    public Menu(MainWindow owner, Editor editor, Runnable compileAction) {
        this.owner = owner;
        this.editor = editor;
        this.compileAction = compileAction;
        preferences = Preferences.userNodeForPackage(MainWindow.class);
        darkMode = preferences.getBoolean("darkMode", false);
        macrosEnabled = preferences.getBoolean("macrosEnabled", true);

        editor.setMacrosEnabled(macrosEnabled);
        buildMenus();
    }

    private void buildMenus() {
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
        compileItem.addActionListener(e -> compileAction.run());

        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_S,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK
        ));
        compileItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_B,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK
        ));

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(exitItem);
        fileMenu.addSeparator();
        fileMenu.add(compileItem);
        add(fileMenu);

        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem("Dark Mode");
        darkModeItem.setSelected(darkMode);
        darkModeItem.addActionListener(e -> {
            darkMode = darkModeItem.isSelected();
            preferences.putBoolean("darkMode", darkMode);
            ThemeManager.applyTheme(owner, editor, darkMode);
        });
        viewMenu.add(darkModeItem);
        add(viewMenu);

        JMenu optionsMenu = new JMenu("Options");
        JCheckBoxMenuItem macrosItem = new JCheckBoxMenuItem("Enable Macros");
        macrosItem.setSelected(macrosEnabled);
        macrosItem.addActionListener(e -> {
            macrosEnabled = macrosItem.isSelected();
            preferences.putBoolean("macrosEnabled", macrosEnabled);
            editor.setMacrosEnabled(macrosEnabled);
        });
        macrosItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_M,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK
        ));
        optionsMenu.add(macrosItem);
        add(optionsMenu);
    }

    public void applyInitialTheme() {
        ThemeManager.applyTheme(owner, editor, darkMode);
    }

    public Path getCurrentFile() {
        return currentFile;
    }

    boolean saveDocument() {
        if (currentFile == null) {
            return saveDocumentAs();
        }

        try {
            Files.writeString(currentFile, editor.getText(), StandardCharsets.UTF_8);
            editor.setDirty(false);
            owner.updateWindowTitle();
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                owner,
                "Could not save file:\n" + e.getMessage(),
                "Save Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    boolean saveDocumentAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save LaTeX document");
        chooser.setFileFilter(new FileNameExtensionFilter("Latex Files (*.tex)", "tex"));

        if (currentFile != null) {
            chooser.setSelectedFile(currentFile.toFile());
        } else {
            chooser.setSelectedFile(new java.io.File("Untitled.tex"));
        }

        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        Path selectedFile = chooser.getSelectedFile().toPath();
        if (!selectedFile.getFileName().toString().contains(".")) {
            selectedFile = selectedFile.resolveSibling(selectedFile.getFileName() + ".tex");
        }

        if (Files.exists(selectedFile) && !selectedFile.equals(currentFile)) {
            int choice = JOptionPane.showConfirmDialog(
                owner,
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
        currentFile = selectedFile;
        return saveDocument();
    }

    private void openDocument() {
        currentFile = editor.openDocument(currentFile, this::saveDocument);
        owner.updateWindowTitle();
    }

    private void newDocument() {
        currentFile = editor.newDocument(currentFile, this::saveDocument);
        owner.updateWindowTitle();
    }

    public void requestClose() {
        if (editor.confirmUnsavedChanges(this::saveDocument)) {
            owner.dispose();
        }
    }
}
