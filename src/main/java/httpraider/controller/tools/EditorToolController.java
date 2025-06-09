package httpraider.controller.tools;

import burp.api.montoya.ui.editor.HttpRequestEditor;
import httpraider.controller.StreamController;
import httpraider.controller.ToolControllerInterface;
import httpraider.view.menuBars.EditorToolsPanel;
import httpraider.view.panels.HTTPEditorPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;

public class EditorToolController implements ToolControllerInterface {
    private final EditorToolsPanel view;
    private final HTTPEditorPanel<HttpRequestEditor> editor;
    private final StreamController streamController;
    private final Timer timer;
    private int lastLen = -1;

    private static final String[] HEADERS = {
            "GET / HTTP/1.1\r\n",
            "Host: ",
            "X-Forwarded-Host: ",
            "X-Forwarded-For: 127.0.0.1\r\n",
            "Content-Length: \r\n",
            "Content-Type: application/x-www-form-urlencoded\r\n",
            "Content-Type: application/json\r\n",
            "Content-Type: multipart/form-data\r\n",
            "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36\r\n",
            "Connection: keep-alive\r\n",
            "Range: bytes=0-10000\r\n",
            "Expect: 100-continue\r\n",
            "Max-Forwards: 0\r\n",
            "Upgrade: \r\n",
            "Accept: */*\r\n",
            "Accept-Encoding: gzip, deflate, br\r\n",
            "Authorization: \r\n",
            "Cache-Control: private, no-cache, no-store\r\n",
            "Origin: \r\n",
            "Referer: \r\n",

    };

    public EditorToolController(EditorToolsPanel view,
                                HTTPEditorPanel<HttpRequestEditor> editor, StreamController streamController) {
        this.view = view;
        this.editor = editor;
        this.streamController = streamController;
        timer = new Timer(50, e -> refresh());
        view.setEnableTagsListener(e->streamController.setTagsEnabled(view.tagsEnabled()));
    }

    @Override public String id() { return "EDITOR"; }

    @Override public String name() { return "Stream Tools"; }

    @Override public JComponent component() { return view; }

    @Override public void attach() {
        timer.start();
        view.setInsertStringActionListener(this::insert);
        view.setInsertHeaderActionListener(this::insertHeader);
    }

    private void refresh() {
        int len = editor.getSelection()
                .map(s -> s.contents().getBytes().length)
                .orElse(0);
        if (len != lastLen) {
            view.setSelectedBytes(len);
            lastLen = len;
        }
    }

    private void insertHeader(ActionEvent e){
        int header = view.getSelectedHeader();
        String payload = HEADERS[view.getSelectedHeader()];
        if (header == 1 || header == 2) payload = payload + streamController.getHost() + "\r\n";
        setBytesAtCaret(payload.getBytes());
    }

    private void insert(ActionEvent e) {
        String ascii = view.getAsciiText();
        if (ascii.isEmpty()) return;

        byte[] payload = ascii.repeat(view.getRepeatCount())
                .getBytes(StandardCharsets.ISO_8859_1);

        setBytesAtCaret(payload);
    }

    private void setBytesAtCaret(byte[] payload){
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
        editor.setCaretPosition(caret + payload.length+1);
    }
}
