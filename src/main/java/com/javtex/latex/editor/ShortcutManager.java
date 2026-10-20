package com.javtex.latex.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.fife.ui.rsyntaxtextarea.CodeTemplateManager;
import org.fife.ui.rsyntaxtextarea.templates.CodeTemplate;
import org.fife.ui.rsyntaxtextarea.templates.StaticCodeTemplate;

public class ShortcutManager {
    private Path fileName;
    private String[][] shortcuts;
    private final CodeTemplateManager ctm;
    private boolean enabled = true;

    public ShortcutManager(Path fileName, CodeTemplateManager ctm) throws IOException {
        this.ctm = ctm;
        loadNewHotkeys(fileName);
    }

    public void loadNewHotkeys() throws IOException {
        shortcuts = createHotkeyMatrix();
        for (String[] shortcut : shortcuts) {
            CodeTemplate ct = new StaticCodeTemplate(shortcut[0], shortcut[1], shortcut[2]);
            ctm.addTemplate(ct);
        }
    }

    public void loadNewHotkeys(Path fileName) throws IOException {
        this.fileName = fileName;
        loadNewHotkeys();
    }

    private String[][] createHotkeyMatrix() throws IOException{
        String[] hotkeys = readHotkeyFile().split(";");
        String[][] formatted = new String[hotkeys.length][3];
        for (int i = 0; i < hotkeys.length; i++) {
            String[] temp = hotkeys[i].split("::=", 2);
            String name = temp[0].replaceAll("\\s", "");
            String expansion = temp[1]
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replaceAll("^\\s+", "");
            String[] temp1 = expansion.split("\\|", 2);
            if (temp1.length == 1) {
                temp1 = new String[]{temp1[0], ""};
            }
            formatted[i] = new String[]{name, temp1[0], temp1[1]};
        }
        return formatted;
    }

    private String readHotkeyFile() throws IOException {
        return Files.readAllLines(fileName)
            .stream()
            .map(line -> {
                int commentStart = line.indexOf("//");

                if (commentStart >= 0) {
                    return line.substring(0, commentStart);
                }

                return line;
            })
            .collect(Collectors.joining("\n"))
            .stripTrailing();
    }

    public boolean expandMacro(Editor editor) {
        if (!enabled) {
            return false;
        }
        int caret = editor.getCaretPosition();

        if (caret == 0) {
            return false;
        }

        try {
            Document doc = editor.getDocument();
            String text = doc.getText(0, caret);

            int start = text.length();

            while (start > 0) {
                char c = text.charAt(start - 1);
                
                if (!Character.isLetterOrDigit(c) && c != '@' && c != '_' && c != '/' && c != '+' && c != '-' && c != '.' && c != '*'
                    && c != '=' && c!= '!' && c != '<' && c != '>') {
                    break;
                }

                start--;
            }

            if (start == caret) {
                return false;
            }

            String word = text.substring(start);

            for (String[] shortcut : shortcuts) {
                if (shortcut[0].equals(word)) {
                    String beforeCaret = shortcut[1];
                    String afterCaret = shortcut[2];

                    doc.remove(start, caret-start);
                    doc.insertString(
                        start,
                        beforeCaret + afterCaret,
                        null
                    );

                    editor.setCaretPosition(start + beforeCaret.length());

                    return true;
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean b) {
        enabled = b;
    }
}
