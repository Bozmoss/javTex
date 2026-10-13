package com.example.latex.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.fife.ui.rsyntaxtextarea.CodeTemplateManager;
import org.fife.ui.rsyntaxtextarea.templates.CodeTemplate;
import org.fife.ui.rsyntaxtextarea.templates.StaticCodeTemplate;

public class ShortcutManager {
    private Path fileName;
    private String[][] shortcuts;
    private final CodeTemplateManager ctm;

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
        String[] hotkeys = readHotkeyFile().replaceAll("\\s", "").split(";");
        String[][] formatted = new String[hotkeys.length][3];
        for (int i = 0; i < hotkeys.length; i++) {
            String[] temp = hotkeys[i].split("::=");
            String[] temp1 = temp[1].split("\\|");
            if (temp1.length == 1) {
                temp1 = new String[]{temp1[0], ""};
            }
            formatted[i] = new String[]{temp[0], temp1[0], temp1[1]};
        }
        System.out.println(Arrays.deepToString(formatted));
        return formatted;
    }

    private String readHotkeyFile() throws IOException {
        return Files.readString(fileName);
    }
}
