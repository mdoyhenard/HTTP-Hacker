package httpraider.controller;

import burp.api.montoya.ui.editor.HttpRequestEditor;
import httpraider.view.menuBars.EditorToolsPanel;
import httpraider.view.panels.HTTPEditorPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;

public class EditorToolController implements ToolControllerInterface{
    private final EditorToolsPanel ui;
    private final HTTPEditorPanel<HttpRequestEditor> editor;
    private final Timer timer;
    private int lastLen = -1;

    public EditorToolController(EditorToolsPanel ui,
                      HTTPEditorPanel<HttpRequestEditor> editor) {
        this.ui = ui;
        this.editor = editor;
        timer = new Timer(50, e -> refresh());
    }

    @Override public String id() { return "EDITOR"; }

    @Override public String name() { return "Stream Tools"; }

    @Override public JComponent component() { return ui; }

    @Override public void attach() {
        timer.start();
        ui.setInsertStringActionListener(this::insert);
    }

    private void refresh() {
        int len = editor.getSelection()
                .map(s -> s.contents().getBytes().length)
                .orElse(0);
        if (len != lastLen) {
            ui.setSelectedBytes(len);
            lastLen = len;
        }
    }

    private void insert(ActionEvent e) {
        String ascii = ui.getAsciiText();
        if (ascii.isEmpty()) return;

        byte[] payload = ascii.repeat(ui.getRepeatCount())
                .getBytes(StandardCharsets.ISO_8859_1);

        byte[] original = editor.getBytes();
        int caret = Math.max(0,
                Math.min(editor.getCaretPosition(), original.length));

        byte[] patched = new byte[original.length + payload.length];
        System.arraycopy(original, 0, patched, 0, caret);
        System.arraycopy(payload,  0, patched, caret, payload.length);
        System.arraycopy(original, caret, patched,
                caret + payload.length,
                original.length - caret);

        editor.setBytes(patched);
        editor.setCaretPosition(caret + payload.length);
    }
}
