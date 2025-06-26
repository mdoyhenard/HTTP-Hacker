package httpraider.controller.tools;

import burp.api.montoya.ui.editor.HttpRequestEditor;
import httpraider.controller.StreamController;
import httpraider.model.CustomTagModel;
import httpraider.view.panels.JSCodeEditorPanel;
import httpraider.view.panels.EditorToolsPanel;
import httpraider.view.panels.HttpEditorPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EditorToolController implements ToolControllerInterface, CustomTagManager.CustomTagListener {


    private final static int HOST_ID = 1;
    private final static int HOST_FORWARD_ID = 2;
    private final static int CL_ID = 4;

    private final EditorToolsPanel view;
    private final HttpEditorPanel<HttpRequestEditor> editor;
    private final StreamController streamController;
    private final Timer timer;
    private final CustomTagManager tagManager = CustomTagManager.getInstance();
    private final Map<CustomTagModel, JPanel> rowPanels = new LinkedHashMap<>();

    private static final String[] HEADERS = {
            "GET / HTTP/1.1\r\n",
            "Host: ",
            "X-Forwarded-Host: ",
            "X-Forwarded-For: 127.0.0.1\r\n",
            "Content-Length: ",
            "Transfer-Encoding: chunked",
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
            "Referer: \r\n"
    };

    public EditorToolController(EditorToolsPanel view,
                                HttpEditorPanel<HttpRequestEditor> editor,
                                StreamController streamController) {
        this.view = view;
        this.editor = editor;
        this.streamController = streamController;
        timer = new Timer(50, e -> refresh());
        view.setEnableTagsListener(e -> streamController.setTagsEnabled(view.tagsEnabled()));
    }

    @Override public String id() { return "EDITOR"; }
    @Override public String name() { return "Stream Tools"; }
    @Override public JComponent component() { return view; }

    @Override
    public void attach() {
        timer.start();
        view.setInsertStringActionListener(this::insert);
        view.setInsertHeaderActionListener(this::insertHeader);
        view.getAddCustomTagButton().addActionListener(this::onAddCustomTagButton);
        tagManager.addListener(this);
        tagsChanged(tagManager.getTags());
    }

    private void refresh() {
        int len = editor.getSelection()
                .map(s -> s.contents().getBytes().length)
                .orElse(0);
        view.setSelectedBytes(len);
    }

    private void insertHeader(ActionEvent e) {
        int idx = view.getSelectedHeader();
        String payload = HEADERS[idx];
        if (idx == HOST_ID || idx == HOST_FORWARD_ID) payload += streamController.getHost() + "\r\n";
        if (idx == CL_ID) payload += getPayloadLength() + "\r\n";
        insertBytes(payload.getBytes());
    }

    private void insert(ActionEvent e) {
        String ascii = view.getAsciiText();
        if (ascii.isEmpty()) return;
        byte[] payload = ascii.repeat(view.getRepeatCount())
                .getBytes(StandardCharsets.ISO_8859_1);
        insertBytes(payload);
    }

    private void insertBytes(byte[] payload) {
        byte[] original = editor.getBytes();
        int caret = Math.max(0, Math.min(editor.getCaretPosition(), original.length));
        byte[] patched = new byte[original.length + payload.length];
        System.arraycopy(original, 0, patched, 0, caret);
        System.arraycopy(payload, 0, patched, caret, payload.length);
        System.arraycopy(original, caret, patched, caret + payload.length,
                original.length - caret);
        editor.setBytes(patched);
        editor.setCaretPosition(caret + payload.length + 1);
    }

    private int getPayloadLength(){
        if (editor.getCaretPosition()<=0) return -1;
        int fromIndex = editor.getCaretPosition();
        byte[] data = editor.getBytes();
        byte[] sequence = { '\r', '\n' };
        int end = data.length - sequence.length + 1;
        for (int i = fromIndex; i < end; i++) {
            boolean found = true;
            for (int j = 0; j < sequence.length; j++) {
                if (data[i + j] != sequence[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return data.length - (i + sequence.length);
            }
        }
        return 0;
    }

    private void onAddCustomTagButton(ActionEvent e) {
        CustomTagModel def = new CustomTagModel("", "");
        tagManager.addTag(def);
    }

    @Override
    public void tagsChanged(List<CustomTagModel> newTags) {
        JPanel container = view.getCustomTagsContainer();

        // remove deleted
        rowPanels.keySet().removeIf(def -> {
            if (!newTags.contains(def)) {
                container.remove(rowPanels.get(def));
                return true;
            }
            return false;
        });

        // add or update existing
        for (CustomTagModel def : newTags) {
            if (!rowPanels.containsKey(def)) {
                JPanel row = createRowPanel(def);
                rowPanels.put(def, row);
                container.add(row);
            } else {
                JPanel row = rowPanels.get(def);
                JTextField tf = (JTextField) row.getComponent(2);
                if (!tf.getText().equals(def.getName())) {
                    tf.setText(def.getName());
                }
            }
        }

        container.revalidate();
        container.repaint();
    }

    private JPanel createRowPanel(CustomTagModel def) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setBorder(new EmptyBorder(4, 0, 4, 0));
        JButton del = new JButton("-");
        JLabel lbl = new JLabel("Tag");
        JTextField tf = new JTextField(def.getName(), 8);
        JButton code = new JButton("Code");

        del.addActionListener(e -> tagManager.removeTag(def));

        tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { }
            private void update() {
                def.setName(tf.getText());
                tagManager.updateTag(def);
            }
        });

        code.addActionListener(e -> {
            JDialog dlg = new JDialog(
                    SwingUtilities.getWindowAncestor(view),
                    "Custom Tag Code",
                    Dialog.ModalityType.APPLICATION_MODAL
            );
            JSCodeEditorPanel area = new JSCodeEditorPanel(def.getScript());
            JScrollPane sp = new JScrollPane(area);
            JButton save = new JButton("Save");
            JButton cancel = new JButton("Cancel");
            save.addActionListener(ae -> {
                def.setScript(area.getCode());
                dlg.dispose();
                tagManager.updateTag(def);
            });
            cancel.addActionListener(ae -> dlg.dispose());
            JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            bar.add(save);
            bar.add(cancel);
            dlg.getContentPane().add(sp, BorderLayout.CENTER);
            dlg.getContentPane().add(bar, BorderLayout.SOUTH);
            dlg.pack();
            dlg.setLocationRelativeTo(view);
            dlg.setVisible(true);
        });

        row.add(del);
        row.add(lbl);
        row.add(tf);
        row.add(code);
        return row;
    }
}
