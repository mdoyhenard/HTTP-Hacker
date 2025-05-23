package httpraider.view.panels;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.editor.Editor;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public class HTTPEditorPanel<T extends Editor> extends JPanel {

    private JLabel name;
    private T editor;
    private final List<Consumer<byte[]>> byteListeners = new ArrayList<>();

    public HTTPEditorPanel(String text, T editor){
        super(new BorderLayout());
        this.editor = editor;
        name = new JLabel(text);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        name.setBorder(BorderFactory.createEmptyBorder(8, 12, 2, 0));
        add(name, BorderLayout.NORTH);
        add(this.editor.uiComponent(), BorderLayout.CENTER);
    }

    public T getEditor(){ return editor; }

    public Component getUiComponent(){ return editor.uiComponent(); }

    public void setName(String text){ name.setText(text); }

    public byte[] getBytes(){
        return (editor instanceof HttpRequestEditor) ? ((HttpRequestEditor)editor).getRequest().toByteArray().getBytes() : ((WebSocketMessageEditor)editor).getContents().getBytes();
    }

    public void setBytes(byte[] data){
        if (editor instanceof HttpRequestEditor)
            ((HttpRequestEditor)editor).setRequest(HttpRequest.httpRequest(((HttpRequestEditor) editor).getRequest().httpService(), ByteArray.byteArray(data)));
        else
            ((WebSocketMessageEditor)editor).setContents(ByteArray.byteArray(data));
        notifyByteListeners(data);
    }

    public void addByteListener(Consumer<byte[]> l){ byteListeners.add(l); }

    private void notifyByteListeners(byte[] data){
        for(Consumer<byte[]> l: byteListeners) l.accept(data);
    }
}
